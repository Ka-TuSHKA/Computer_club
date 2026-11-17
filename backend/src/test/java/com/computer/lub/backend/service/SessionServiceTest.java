package com.computer.lub.backend.service;

import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.entity.*;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PcRepository pcRepository;
    @Mock private UserRepository userRepository;
    @Mock private PcStatusService pcStatusService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SessionService sessionService;

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
        mockPc.setStatus(PcStatus.FREE);

        mockBooking = new Booking();
        mockBooking.setId(100L);
        mockBooking.setPc(mockPc);
        mockBooking.setUser(mockUser);
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setStartTime(LocalDateTime.now().minusMinutes(10));
        mockBooking.setEndTime(LocalDateTime.now().plusHours(1));
    }

    @Test
    void shouldReturnAllSessionsForAdmin() {
        Session session = new Session();
        session.setId(1L);
        session.setPc(mockPc);
        session.setUser(mockUser);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);

        when(sessionRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(session));

        List<SessionResponse> response = sessionService.getSessions("admin", true);

        assertEquals(1, response.size());
        verify(sessionRepository).findAllByOrderByStartTimeDesc();
    }

    @Test
    void shouldStartSessionSuccessfully() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));
        when(sessionRepository.existsByPcIdAndStatus(10L, SessionStatus.ACTIVE)).thenReturn(false);
        when(pcStatusService.syncStatus(mockPc)).thenReturn(true);

        sessionService.startSession(100L);

        assertEquals(BookingStatus.COMPLETED, mockBooking.getStatus());
        verify(sessionRepository).save(any(Session.class));
        verify(bookingRepository).save(mockBooking);
        verify(messagingTemplate).convertAndSend(eq("/topic/pcs"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/bookings"), anyString());
    }

    @Test
    void shouldNotStartSessionOnRepairPc() {
        mockPc.setStatus(PcStatus.REPAIR);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.startSession(100L);
        });
        assertEquals("Нельзя начать сеанс на сломанном ПК или ПК с активной сессией", exception.getMessage());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldNotStartSessionOnCancelledBooking() {
        mockBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.startSession(100L);
        });
        assertEquals("Нельзя начать сеанс по неактивной брони", exception.getMessage());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldFinishSessionSuccessfully() {
        Session activeSession = new Session();
        activeSession.setId(1L);
        activeSession.setPc(mockPc);
        activeSession.setBooking(mockBooking);
        activeSession.setStartTime(LocalDateTime.now().minusHours(1));
        activeSession.setStatus(SessionStatus.ACTIVE);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        sessionService.finishSession(1L);

        assertEquals(SessionStatus.FINISHED, activeSession.getStatus());
        assertEquals(BookingStatus.COMPLETED, mockBooking.getStatus());
        verify(sessionRepository).save(activeSession);
        verify(bookingRepository).save(mockBooking);
        verify(messagingTemplate).convertAndSend(eq("/topic/pcs"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/bookings"), anyString());
    }

    @Test
    void shouldNotFinishAlreadyFinishedSession() {
        Session finishedSession = new Session();
        finishedSession.setId(1L);
        finishedSession.setPc(mockPc);
        finishedSession.setStatus(SessionStatus.FINISHED);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(finishedSession));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.finishSession(1L);
        });
        assertEquals("Сеанс уже завершен", exception.getMessage());
        verify(sessionRepository, never()).save(any(Session.class));
    }
}
