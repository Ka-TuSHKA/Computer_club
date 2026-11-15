package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.dto.BookingResponse;
import com.computer.lub.backend.dto.BusySlotResponse;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final PcRepository pcRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Booking> bookings = isAdmin
                ? bookingRepository.findAllByOrderByStartTimeDesc()
                : bookingRepository.findByUserUsernameOrderByStartTimeDesc(username);

        return ResponseEntity.ok(mapToResponse(bookings));
    }

    @GetMapping("/pc/{pcId}/busy-slots")
    public ResponseEntity<List<BusySlotResponse>> getBusySlots(@PathVariable Long pcId) {
        List<BusySlotResponse> busySlots = new ArrayList<>();

        bookingRepository.findByPcIdAndStatusIn(pcId, List.of("PENDING", "CONFIRMED"))
            .forEach(b -> busySlots.add(new BusySlotResponse(
                b.getStartTime(),
                b.getEndTime(),
                b.getUser().getUsername()
            )));

        sessionRepository.findByPcIdAndStatus(pcId, "ACTIVE")
            .forEach(s -> {
                LocalDateTime endOfDay = s.getStartTime().toLocalDate().atTime(23, 0);
                busySlots.add(new BusySlotResponse(
                    s.getStartTime(),
                    endOfDay,
                    s.getUser().getUsername()
                ));
            });

        return ResponseEntity.ok(busySlots);
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Pc pc = pcRepository.findById(request.getPcId()).orElseThrow();

        LocalDateTime startTime = LocalDateTime.of(request.getDate(), request.getStartTime());
        LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());

        // ЗАЩИТА: Время окончания должно быть позже времени начала
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            return ResponseEntity.badRequest().body("Время окончания должно быть позже времени начала");
        }

        // ЗАЩИТА ОТ ПРОШЛОГО ВРЕМЕНИ
        if (startTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Нельзя забронировать время в прошлом");
        }

        // Проверка пересечений (овербукинг)
        boolean isOverlapping = bookingRepository.findByPcIdAndStatusIn(pc.getId(), List.of("PENDING", "CONFIRMED"))
                .stream().anyMatch(b -> startTime.isBefore(b.getEndTime()) && endTime.isAfter(b.getStartTime()));

        if (isOverlapping) {
            return ResponseEntity.badRequest().body("Это время уже занято");
        }

        Booking booking = new Booking();
        booking.setPc(pc);
        booking.setUser(user);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus("PENDING");
        bookingRepository.save(booking);

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(startTime) && now.isBefore(endTime)) {
            pc.setStatus("BOOKED");
            pcRepository.save(pc);
        }

        messagingTemplate.convertAndSend("/topic/pcs", "update");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        if ("CANCELLED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body("Нельзя подтвердить неактивную бронь");
        }

        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
        messagingTemplate.convertAndSend("/topic/pcs", "update");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Booking booking = bookingRepository.findById(id).orElseThrow();

        // Клиент может отменять только СВОИ брони
        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).body("Вы можете отменять только свои брони");
        }

        if ("CANCELLED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body("Бронь уже отменена или завершена");
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        Pc pc = booking.getPc();
        if (!"REPAIR".equals(pc.getStatus())) {
            pc.setStatus("FREE");
            pcRepository.save(pc);
        }
        messagingTemplate.convertAndSend("/topic/pcs", "update");
        return ResponseEntity.ok().build();
    }

    private List<BookingResponse> mapToResponse(List<Booking> bookings) {
        return bookings.stream().map(b -> {
            BookingResponse dto = new BookingResponse();
            dto.setId(b.getId());
            dto.setPcNumber(b.getPc().getPcNumber());
            dto.setUsername(b.getUser().getUsername());
            dto.setStartTime(b.getStartTime());
            dto.setEndTime(b.getEndTime());
            dto.setStatus(b.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void autoConfirmBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> activeBookings = bookingRepository.findAll().stream()
            .filter(b -> ("PENDING".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()))
                      && b.getStartTime().isBefore(now)
                      && b.getEndTime().isAfter(now))
            .collect(Collectors.toList());

        boolean hasChanges = false;

        for (Booking b : activeBookings) {
            if ("PENDING".equals(b.getStatus())) {
                b.setStatus("CONFIRMED");
                bookingRepository.save(b);
                hasChanges = true;
            }

            Pc pc = b.getPc();
            if (!"BOOKED".equals(pc.getStatus())) {
                pc.setStatus("BOOKED");
                pcRepository.save(pc);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            messagingTemplate.convertAndSend("/topic/pcs", "update");
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void autoCancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Ищем брони, которые подтверждены или в рассмотрении, но время окончания уже прошло
        List<Booking> expiredBookings = bookingRepository.findAll().stream()
            .filter(b -> ("PENDING".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()))
                      && b.getEndTime().isBefore(now))
            .collect(Collectors.toList());

        boolean hasChanges = false;

        for (Booking b : expiredBookings) {
            b.setStatus("CANCELLED"); // Отменяем просроченную бронь
            bookingRepository.save(b);

            Pc pc = b.getPc();
            // Проверяем, есть ли на этом ПК другие активные брони или сессии
            boolean hasOtherBookings = bookingRepository.findByPcIdAndStatusIn(pc.getId(), List.of("PENDING", "CONFIRMED"))
                .stream().anyMatch(other -> !other.getId().equals(b.getId()));
            boolean hasActiveSession = !sessionRepository.findByPcIdAndStatus(pc.getId(), "ACTIVE").isEmpty();

            // Если других причин быть занятым нет, освобождаем ПК
            if (!hasOtherBookings && !hasActiveSession && !"REPAIR".equals(pc.getStatus())) {
                pc.setStatus("FREE");
                pcRepository.save(pc);
            }
            hasChanges = true;
        }

        if (hasChanges) {
            messagingTemplate.convertAndSend("/topic/pcs", "update");
        }
    }
}
