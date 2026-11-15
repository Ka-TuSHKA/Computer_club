package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PcRepository pcRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private SessionController sessionController;

    private User mockUser;
    private Pc mockPc;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("client");

        mockPc = new Pc();
        mockPc.setId(10L);
        mockPc.setPcNumber("PC-101");
        mockPc.setStatus("FREE");

        mockBooking = new Booking();
        mockBooking.setId(100L);
        mockBooking.setPc(mockPc);
        mockBooking.setUser(mockUser);
        mockBooking.setStatus("CONFIRMED");
        // ВАЖНО: Задаем время брони, чтобы не было NPE при проверке
        mockBooking.setStartTime(LocalDateTime.now().minusMinutes(10));
        mockBooking.setEndTime(LocalDateTime.now().plusHours(1));

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("client");
        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();
    }

    @Test
    void shouldReturnAllSessionsForAdmin() {
        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        Session session = new Session();
        session.setId(1L);
        session.setPc(mockPc);
        session.setUser(mockUser);
        session.setStartTime(LocalDateTime.now());
        session.setStatus("ACTIVE");

        when(sessionRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(session));

        ResponseEntity<List<SessionResponse>> response = sessionController.getSessions();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(sessionRepository).findAllByOrderByStartTimeDesc();
    }

    @Test
    void shouldReturnOnlyUserSessionsForUser() {
        Session session = new Session();
        session.setId(1L);
        session.setPc(mockPc);
        session.setUser(mockUser);
        session.setStartTime(LocalDateTime.now());
        session.setStatus("ACTIVE");

        when(sessionRepository.findByUserUsernameOrderByStartTimeDesc("client")).thenReturn(List.of(session));

        ResponseEntity<List<SessionResponse>> response = sessionController.getSessions();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(sessionRepository).findByUserUsernameOrderByStartTimeDesc("client");
    }

    @Test
    void shouldStartSessionSuccessfully() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        ResponseEntity<?> response = sessionController.startSession(100L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("BOOKED", mockPc.getStatus());
        verify(pcRepository).save(mockPc);
        verify(sessionRepository).save(argThat(s -> "ACTIVE".equals(s.getStatus())));
        verify(messagingTemplate).convertAndSend(eq("/topic/pcs"), anyString());
    }

    @Test
    void shouldNotStartSessionOnRepairPc() {
        mockPc.setStatus("REPAIR");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        ResponseEntity<?> response = sessionController.startSession(100L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя начать сеанс на занятом или сломанном ПК", response.getBody());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldNotStartSessionOnBookedPc() {
        mockPc.setStatus("BOOKED");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        ResponseEntity<?> response = sessionController.startSession(100L);

        assertEquals(400, response.getStatusCode().value());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldNotStartSessionOnCancelledBooking() {
        mockBooking.setStatus("CANCELLED");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        ResponseEntity<?> response = sessionController.startSession(100L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя начать сеанс по неактивной брони", response.getBody());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldFinishSessionSuccessfully() {
        Session activeSession = new Session();
        activeSession.setId(1L);
        activeSession.setPc(mockPc);
        activeSession.setBooking(mockBooking);
        activeSession.setStartTime(LocalDateTime.now().minusHours(1));
        activeSession.setStatus("ACTIVE");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        ResponseEntity<?> response = sessionController.finishSession(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("FINISHED", activeSession.getStatus());
        assertEquals("FREE", mockPc.getStatus());
        assertEquals("COMPLETED", mockBooking.getStatus());
    }

    @Test
    void shouldNotFinishAlreadyFinishedSession() {
        Session finishedSession = new Session();
        finishedSession.setId(1L);
        finishedSession.setPc(mockPc);
        finishedSession.setStatus("FINISHED");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(finishedSession));

        ResponseEntity<?> response = sessionController.finishSession(1L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Сеанс уже завершен", response.getBody());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldFinishSessionWithoutBooking() {
        Session session = new Session();
        session.setId(1L);
        session.setPc(mockPc);
        session.setBooking(null);
        session.setStatus("ACTIVE");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseEntity<?> response = sessionController.finishSession(1L);

        assertEquals(200, response.getStatusCode().value());
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
