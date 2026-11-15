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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final SimpMessagingTemplate messagingTemplate;

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

    @PostMapping("/start/{bookingId}")
    public ResponseEntity<?> startSession(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Pc pc = booking.getPc();
        LocalDateTime now = LocalDateTime.now();

        // ЗАЩИТА: Нельзя начать сеанс раньше времени брони
        if (now.isBefore(booking.getStartTime())) {
            return ResponseEntity.badRequest().body("Еще не наступило время брони");
        }

        // ЗАЩИТА: Нельзя начать сеанс позже времени брони (бронь уже истекла)
        if (now.isAfter(booking.getEndTime())) {
            return ResponseEntity.badRequest().body("Время брони уже истекло");
        }

        // ЗАЩИТА: Нельзя начать сеанс на сломанном или уже занятом ПК
        if ("REPAIR".equals(pc.getStatus()) || "BOOKED".equals(pc.getStatus())) {
            return ResponseEntity.badRequest().body("Нельзя начать сеанс на занятом или сломанном ПК");
        }

        // ЗАЩИТА: Нельзя начать сеанс по отмененной или уже завершенной брони
        if ("CANCELLED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body("Нельзя начать сеанс по неактивной брони");
        }

        Session session = new Session();
        session.setPc(pc);
        session.setUser(booking.getUser());
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus("ACTIVE");
        sessionRepository.save(session);

        pc.setStatus("BOOKED");
        pcRepository.save(pc);

        messagingTemplate.convertAndSend("/topic/pcs", "update");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/finish")
    public ResponseEntity<?> finishSession(@PathVariable Long id) {
        Session session = sessionRepository.findById(id).orElseThrow();

        // ЗАЩИТА: Нельзя завершить уже завершенный сеанс (идемпотентность)
        if ("FINISHED".equals(session.getStatus())) {
            return ResponseEntity.badRequest().body("Сеанс уже завершен");
        }

        session.setStatus("FINISHED");
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        if (session.getBooking() != null) {
            Booking booking = session.getBooking();
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
        }

        Pc pc = session.getPc();
        pc.setStatus("FREE");
        pcRepository.save(pc);

        messagingTemplate.convertAndSend("/topic/pcs", "update");
        return ResponseEntity.ok().build();
    }

    // АВТОМАТИЧЕСКОЕ ЗАВЕРШЕНИЕ СЕАНСОВ
    // Проверяет каждую минуту, не истекло ли время брони у активных сеансов
    @Scheduled(fixedRate = 60000)
    public void autoFinishExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        // Ищем все активные сеансы
        List<Session> activeSessions = sessionRepository.findAll().stream()
            .filter(s -> "ACTIVE".equals(s.getStatus()))
            .collect(Collectors.toList());

        boolean hasChanges = false;

        for (Session session : activeSessions) {
            if (session.getBooking() != null) {
                Booking booking = session.getBooking();

                // Если текущее время больше, чем время окончания брони
                if (now.isAfter(booking.getEndTime())) {

                    // 1. Завершаем сеанс
                    session.setStatus("FINISHED");
                    session.setEndTime(booking.getEndTime());
                    sessionRepository.save(session);

                    // 2. Завершаем бронь
                    booking.setStatus("COMPLETED");
                    bookingRepository.save(booking);

                    // 3. Освобождаем ПК
                    Pc pc = session.getPc();
                    pc.setStatus("FREE");
                    pcRepository.save(pc);

                    hasChanges = true;
                }
            }
        }

        // Если были изменения, отправляем WebSocket-сигнал, чтобы карта обновилась у всех
        if (hasChanges) {
            messagingTemplate.convertAndSend("/topic/pcs", "update");
        }
    }
}
