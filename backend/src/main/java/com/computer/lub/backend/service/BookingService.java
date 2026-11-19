package com.computer.lub.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.dto.BookingResponse;
import com.computer.lub.backend.dto.BusySlotResponse;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.SessionStatus;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.event.AuditEvent;
import com.computer.lub.backend.exception.NoAccessException;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PcRepository pcRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PcStatusService pcStatusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(String username, boolean isAdmin) {
        List<Booking> bookings = isAdmin
                ? bookingRepository.findAllByOrderByStartTimeDesc()
                : bookingRepository.findByUserUsernameOrderByStartTimeDesc(username);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BusySlotResponse> getBusySlots(Long pcId) {
        List<BusySlotResponse> busySlots = new ArrayList<>();

        bookingRepository.findByPcIdAndStatusIn(pcId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .forEach(b -> busySlots.add(new BusySlotResponse(
                        b.getStartTime(),
                        b.getEndTime(),
                        b.getUser().getUsername()
                )));

        sessionRepository.findByPcIdAndStatus(pcId, SessionStatus.ACTIVE)
                .forEach(s -> {
                    LocalDateTime end = s.getEndTime() != null
                            ? s.getEndTime()
                            : s.getStartTime().toLocalDate().atTime(23, 0);
                    busySlots.add(new BusySlotResponse(s.getStartTime(), end, s.getUser().getUsername()));
                });

        return busySlots;
    }

    @Transactional
    public void create(BookingRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Pc pc = pcRepository.findById(request.getPcId()).orElseThrow();

        if (pc.getStatus() == PcStatus.REPAIR) {
            throw new IllegalArgumentException("Нельзя забронировать компьютер в ремонте");
        }

        LocalDateTime startTime = LocalDateTime.of(request.getDate(), request.getStartTime());
        LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Нельзя забронировать время в прошлом");
        }

        boolean overlapsBooking = bookingRepository
                .findByPcIdAndStatusIn(pc.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .anyMatch(b -> startTime.isBefore(b.getEndTime()) && endTime.isAfter(b.getStartTime()));

        boolean overlapsSession = sessionRepository.findByPcIdAndStatus(pc.getId(), SessionStatus.ACTIVE)
                .stream()
                .anyMatch(s -> {
                    LocalDateTime sessionEnd = s.getEndTime() != null
                            ? s.getEndTime()
                            : s.getStartTime().toLocalDate().atTime(23, 0);
                    return startTime.isBefore(sessionEnd) && endTime.isAfter(s.getStartTime());
                });

        if (overlapsBooking || overlapsSession) {
            throw new IllegalArgumentException("Это время уже занято");
        }

        Booking booking = new Booking();
        booking.setPc(pc);
        booking.setUser(user);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        pcStatusService.syncStatus(pc);
        messagingTemplate.convertAndSend("/topic/bookings", "update");

        eventPublisher.publishEvent(new AuditEvent(this, "CREATE_BOOKING", username, "pcId=" + pc.getId() + ", start=" + startTime));
    }

    @Transactional
    public void confirm(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Нельзя подтвердить неактивную бронь");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        pcStatusService.syncStatus(booking.getPc());
        messagingTemplate.convertAndSend("/topic/bookings", "update");

        eventPublisher.publishEvent(new AuditEvent(this, "CONFIRM_BOOKING", booking.getUser().getUsername(), "bookingId=" + id));
    }

    @Transactional
    public void cancel(Long id, String username, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new NoAccessException("Вы можете отменять только свои брони");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Бронь уже отменена или завершена");
        }

        if (sessionRepository.existsByBookingIdAndStatus(booking.getId(), SessionStatus.ACTIVE)) {
            throw new IllegalArgumentException("Нельзя отменить бронь, по которой уже запущен сеанс");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        pcStatusService.syncStatus(booking.getPc());
        messagingTemplate.convertAndSend("/topic/bookings", "update");

        eventPublisher.publishEvent(new AuditEvent(this, "CANCEL_BOOKING", username, "bookingId=" + id));
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoConfirmBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> currentBookings = bookingRepository
                .findByStatusIn(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .filter(b -> !b.getStartTime().isAfter(now) && b.getEndTime().isAfter(now))
                .collect(Collectors.toList());
        if (currentBookings.isEmpty()) {
            return;
        }

        boolean statusChanged = false;
        Set<Pc> affectedPcs = new HashSet<>();

        for (Booking b : currentBookings) {
            if (b.getStatus() == BookingStatus.PENDING) {
                b.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(b);
                statusChanged = true;
            }
            affectedPcs.add(b.getPc());
        }

        boolean pcChanged = affectedPcs.stream().anyMatch(pcStatusService::syncStatus);
        if (statusChanged || pcChanged) {
            messagingTemplate.convertAndSend("/topic/pcs", "update");
            messagingTemplate.convertAndSend("/topic/bookings", "update");
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings = bookingRepository
                .findByStatusIn(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .filter(b -> !b.getEndTime().isAfter(now))
                .collect(Collectors.toList());
        if (expiredBookings.isEmpty()) {
            return;
        }

        boolean statusChanged = false;
        Set<Pc> affectedPcs = new HashSet<>();

        for (Booking b : expiredBookings) {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            statusChanged = true;
            affectedPcs.add(b.getPc());
        }

        boolean pcChanged = affectedPcs.stream().anyMatch(pcStatusService::syncStatus);
        if (statusChanged || pcChanged) {
            messagingTemplate.convertAndSend("/topic/pcs", "update");
            messagingTemplate.convertAndSend("/topic/bookings", "update");
        }
    }

    private BookingResponse mapToResponse(Booking b) {
        BookingResponse dto = new BookingResponse();
        dto.setId(b.getId());
        dto.setPcNumber(b.getPc().getPcNumber());
        dto.setUsername(b.getUser().getUsername());
        dto.setStartTime(b.getStartTime());
        dto.setEndTime(b.getEndTime());
        dto.setStatus(b.getStatus());
        return dto;
    }
}
