package com.computer.lub.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.dto.WalkInSessionRequest;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.SessionStatus;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final PcRepository pcRepository;
    private final UserRepository userRepository;
    private final PcStatusService pcStatusService;
    private final SimpMessagingTemplate messagingTemplate;

    public List<SessionResponse> getSessions(String username, boolean isAdmin) {
        List<Session> sessions = isAdmin
                ? sessionRepository.findAllByOrderByStartTimeDesc()
                : sessionRepository.findByUserUsernameOrderByStartTimeDesc(username);
        return sessions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void startSession(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Pc pc = booking.getPc();
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(booking.getStartTime())) {
            throw new IllegalArgumentException("Еще не наступило время брони");
        }
        if (now.isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("Время брони уже истекло");
        }
        boolean pcBusy = pc.getStatus() == PcStatus.REPAIR
                || sessionRepository.existsByPcIdAndStatus(pc.getId(), SessionStatus.ACTIVE);
        if (pcBusy) {
            throw new IllegalArgumentException("Нельзя начать сеанс на сломанном ПК или ПК с активной сессией");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Нельзя начать сеанс по неактивной брони");
        }

        Session session = new Session();
        session.setPc(pc);
        session.setUser(booking.getUser());
        session.setBooking(booking);
        session.setStartTime(now);
        session.setEndTime(booking.getEndTime());
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(session);

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        pcStatusService.syncStatus(pc);
        messagingTemplate.convertAndSend("/topic/pcs", "update");
        messagingTemplate.convertAndSend("/topic/bookings", "update");
    }

    @Transactional
    public void startWalkInSession(WalkInSessionRequest request) {
        Pc pc = pcRepository.findById(request.getPcId()).orElseThrow();
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с таким логином не найден");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime plannedEnd = now.plusMinutes(request.getDuration());

        if (pc.getStatus() == PcStatus.REPAIR) {
            throw new IllegalArgumentException("Компьютер находится в ремонте");
        }
        if (sessionRepository.existsByPcIdAndStatus(pc.getId(), SessionStatus.ACTIVE)) {
            throw new IllegalArgumentException("На компьютере уже идет сеанс");
        }

        boolean overlapsBooking = bookingRepository
                .findByPcIdAndStatusIn(pc.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .anyMatch(b -> now.isBefore(b.getEndTime()) && plannedEnd.isAfter(b.getStartTime()));
        if (overlapsBooking) {
            throw new IllegalArgumentException("Компьютер забронирован на выбранное время");
        }

        Session session = new Session();
        session.setPc(pc);
        session.setUser(user);
        session.setBooking(null);
        session.setStartTime(now);
        session.setEndTime(plannedEnd);
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(session);

        pcStatusService.syncStatus(pc);
        messagingTemplate.convertAndSend("/topic/pcs", "update");
    }

    @Transactional
    public void finishSession(Long id) {
        Session session = sessionRepository.findById(id).orElseThrow();

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new IllegalArgumentException("Сеанс уже завершен");
        }

        session.setStatus(SessionStatus.FINISHED);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        if (session.getBooking() != null) {
            Booking booking = session.getBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        }

        pcStatusService.syncStatus(session.getPc());
        messagingTemplate.convertAndSend("/topic/pcs", "update");
        messagingTemplate.convertAndSend("/topic/bookings", "update");
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoFinishExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<Session> expired = sessionRepository.findByStatus(SessionStatus.ACTIVE)
                .stream()
                .filter(s -> s.getEndTime() != null && !s.getEndTime().isAfter(now))
                .collect(Collectors.toList());
        if (expired.isEmpty()) {
            return;
        }

        for (Session session : expired) {
            session.setStatus(SessionStatus.FINISHED);
            sessionRepository.save(session);

            if (session.getBooking() != null) {
                Booking booking = session.getBooking();
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
            }
            pcStatusService.syncStatus(session.getPc());
        }
        messagingTemplate.convertAndSend("/topic/pcs", "update");
        messagingTemplate.convertAndSend("/topic/bookings", "update");
    }

    private SessionResponse mapToResponse(Session s) {
        SessionResponse dto = new SessionResponse();
        dto.setId(s.getId());
        dto.setPcNumber(s.getPc().getPcNumber());
        dto.setUsername(s.getUser().getUsername());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setStatus(s.getStatus());
        return dto;
    }
}
