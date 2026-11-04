package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.dto.BookingResponse;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final PcRepository pcRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Booking> bookings = isAdmin
                ? bookingRepository.findAllByOrderByStartTimeDesc()
                : bookingRepository.findByUserUsernameOrderByStartTimeDesc(username);

        List<BookingResponse> response = bookings.stream().map(b -> {
            BookingResponse dto = new BookingResponse();
            dto.setId(b.getId());
            dto.setPcNumber(b.getPc().getPcNumber());
            dto.setUsername(b.getUser().getUsername());
            dto.setStartTime(b.getStartTime());
            dto.setEndTime(b.getEndTime());
            dto.setStatus(b.getStatus());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Pc pc = pcRepository.findById(request.getPcId()).orElseThrow();

        Booking booking = new Booking();
        booking.setPc(pc);
        booking.setUser(user);
        booking.setStartTime(LocalDateTime.of(request.getDate(), request.getStartTime()));
        booking.setEndTime(LocalDateTime.of(request.getDate(), request.getStartTime()).plusMinutes(request.getDurationMinutes()));
        booking.setStatus("PENDING"); // Статус "В рассмотрении"
        bookingRepository.save(booking);

        // Чтобы другие видели, что место занято (в будущем, тут должна быть проверка по времени)
        pc.setStatus("BOOKED");
        pcRepository.save(pc);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        // Освобождаем ПК (в реальной системе нужно проверять, нет ли других броней на это время)
        Pc pc = booking.getPc();
        pc.setStatus("FREE");
        pcRepository.save(pc);

        return ResponseEntity.ok().build();
    }
}
