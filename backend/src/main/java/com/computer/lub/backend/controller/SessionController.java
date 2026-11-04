package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final PcRepository pcRepository;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getSessions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Session> sessions = isAdmin
                ? sessionRepository.findAllByOrderByStartTimeDesc()
                : sessionRepository.findByUserUsernameOrderByStartTimeDesc(username);

        List<SessionResponse> response = sessions.stream().map(s -> {
            SessionResponse dto = new SessionResponse();
            dto.setId(s.getId());
            dto.setPcNumber(s.getPc().getPcNumber());
            dto.setUsername(s.getUser().getUsername());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setStatus(s.getStatus());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // Начать сеанс по ID брони
    @PostMapping("/start/{bookingId}")
    public ResponseEntity<?> startSession(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        Session session = new Session();
        session.setPc(booking.getPc());
        session.setUser(booking.getUser());
        session.setStartTime(LocalDateTime.now());
        session.setStatus("ACTIVE");
        sessionRepository.save(session);

        // ПК становится занятым
        Pc pc = booking.getPc();
        pc.setStatus("BOOKED");
        pcRepository.save(pc);

        // Саму бронь помечаем как завершенную (сеанс уже начат)
        booking.setStatus("COMPLETED");
        bookingRepository.save(booking);

        return ResponseEntity.ok().build();
    }

    // Завершить сеанс
    @PutMapping("/{id}/finish")
    public ResponseEntity<?> finishSession(@PathVariable Long id) {
        Session session = sessionRepository.findById(id).orElseThrow();
        session.setStatus("FINISHED");
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        // Освобождаем ПК
        Pc pc = session.getPc();
        pc.setStatus("FREE");
        pcRepository.save(pc);

        return ResponseEntity.ok().build();
    }
}
