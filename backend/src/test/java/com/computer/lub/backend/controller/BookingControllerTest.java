package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.User;
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
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PcRepository pcRepository;
    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private BookingController bookingController;

    private User mockUser;
    private Pc mockPc;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUsername("client");
        mockUser.setId(1L);

        mockPc = new Pc();
        mockPc.setId(10L);
        mockPc.setPcNumber("PC-101");
        mockPc.setStatus("FREE");

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("client");
        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();
    }

    @Test
    void shouldCreateBookingSuccessfully() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(mockUser));
        when(pcRepository.findById(10L)).thenReturn(Optional.of(mockPc));
        when(bookingRepository.findByPcIdAndStatusIn(anyLong(), anyList())).thenReturn(Collections.emptyList());

        BookingRequest request = new BookingRequest();
        request.setPcId(10L);
        request.setDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(14, 0));
        request.setDurationMinutes(60);

        ResponseEntity<?> response = bookingController.createBooking(request);

        assertEquals(200, response.getStatusCode().value());
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/pcs"), anyString());
    }

    @Test
    void shouldRejectBookingIfEndTimeBeforeStartTime() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(mockUser));
        when(pcRepository.findById(10L)).thenReturn(Optional.of(mockPc));

        BookingRequest request = new BookingRequest();
        request.setPcId(10L);
        request.setDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(14, 0));
        request.setDurationMinutes(-60);

        ResponseEntity<?> response = bookingController.createBooking(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Время окончания должно быть позже времени начала", response.getBody());
    }

    @Test
    void shouldRejectBookingInThePast() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(mockUser));
        when(pcRepository.findById(10L)).thenReturn(Optional.of(mockPc));

        BookingRequest request = new BookingRequest();
        request.setPcId(10L);
        request.setDate(LocalDate.now().minusDays(1));
        request.setStartTime(LocalTime.of(14, 0));
        request.setDurationMinutes(60);

        ResponseEntity<?> response = bookingController.createBooking(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя забронировать время в прошлом", response.getBody());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldRejectOverlappingBooking() {
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(mockUser));
        when(pcRepository.findById(10L)).thenReturn(Optional.of(mockPc));

        Booking existing = new Booking();
        existing.setStartTime(LocalDateTime.of(2030, 1, 1, 12, 0));
        existing.setEndTime(LocalDateTime.of(2030, 1, 1, 14, 0));
        when(bookingRepository.findByPcIdAndStatusIn(anyLong(), anyList())).thenReturn(List.of(existing));

        BookingRequest request = new BookingRequest();
        request.setPcId(10L);
        request.setDate(LocalDate.of(2030, 1, 1));
        request.setStartTime(LocalTime.of(13, 0));
        request.setDurationMinutes(60);

        ResponseEntity<?> response = bookingController.createBooking(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Это время уже занято", response.getBody());
    }

    @Test
    void shouldConfirmBooking() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStatus("PENDING");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.confirmBooking(100L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CONFIRMED", booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldNotConfirmCancelledBooking() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStatus("CANCELLED");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.confirmBooking(100L);

        assertEquals(400, response.getStatusCode().value());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldCancelBookingAndFreePc() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStatus("CONFIRMED");
        booking.setPc(mockPc);
        booking.setUser(mockUser); // <-- ДОБАВЛЕНО

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.cancelBooking(100L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("FREE", mockPc.getStatus());
        verify(pcRepository).save(mockPc);
    }

    @Test
    void shouldNotCancelCompletedBooking() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStatus("COMPLETED");
        booking.setUser(mockUser); // <-- ДОБАВЛЕНО

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.cancelBooking(100L);

        assertEquals(400, response.getStatusCode().value());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldAutoConfirmPendingBookingWhenTimeComes() {
        Booking booking = new Booking();
        booking.setStatus("PENDING");
        booking.setPc(mockPc);
        booking.setStartTime(LocalDateTime.now().minusMinutes(10));
        booking.setEndTime(LocalDateTime.now().plusMinutes(50));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingController.autoConfirmBookings();

        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("BOOKED", mockPc.getStatus());
        verify(messagingTemplate).convertAndSend(eq("/topic/pcs"), anyString());
    }

    @Test
    void shouldNotAutoConfirmFutureBooking() {
        Booking booking = new Booking();
        booking.setStatus("PENDING");
        booking.setPc(mockPc);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingController.autoConfirmBookings();

        assertEquals("PENDING", booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldBeIdempotentWhenSchedulerRunsTwice() {
        Booking booking = new Booking();
        booking.setStatus("PENDING");
        booking.setPc(mockPc);
        booking.setStartTime(LocalDateTime.now().minusMinutes(10));
        booking.setEndTime(LocalDateTime.now().plusMinutes(50));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        bookingController.autoConfirmBookings();
        bookingController.autoConfirmBookings();

        verify(bookingRepository, times(1)).save(booking);
        verify(pcRepository, times(1)).save(mockPc);
    }

    @Test
    void shouldReturnBusySlots() {
        Booking booking = new Booking();
        booking.setStartTime(LocalDateTime.now());
        booking.setEndTime(LocalDateTime.now().plusHours(1));
        booking.setUser(mockUser);

        Session session = new Session();
        session.setStartTime(LocalDateTime.now());
        session.setUser(mockUser);

        when(bookingRepository.findByPcIdAndStatusIn(eq(10L), anyList())).thenReturn(List.of(booking));
        when(sessionRepository.findByPcIdAndStatus(10L, "ACTIVE")).thenReturn(List.of(session));

        ResponseEntity<?> response = bookingController.getBusySlots(10L);

        assertEquals(200, response.getStatusCode().value());
        List<?> body = (List<?>) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
}
