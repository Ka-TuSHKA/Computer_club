package com.computer.lub.backend.service;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PcRepository pcRepository;
    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PcStatusService pcStatusService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void shouldCreateBookingWhenTimeIsFree() {
        User user = new User();
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus(PcStatus.FREE);

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(pcRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(sessionRepository.findByPcIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());

        BookingRequest request = new BookingRequest();
        request.setPcId(1L);
        request.setDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(12, 0));
        request.setDurationMinutes(60);

        bookingService.create(request, "client");

        verify(bookingRepository).save(any(Booking.class));
        verify(messagingTemplate).convertAndSend("/topic/bookings", "update");
    }

    @Test
    void shouldThrowExceptionWhenBookingInRepairPc() {
        Pc pc = new Pc();
        pc.setStatus(PcStatus.REPAIR);

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(new User()));
        when(pcRepository.findById(1L)).thenReturn(Optional.of(pc));

        BookingRequest request = new BookingRequest();
        request.setPcId(1L);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(request, "client"));
    }

    @Test
    void shouldAutoCancelExpiredBookings() {
        Booking expiredBooking = new Booking();
        expiredBooking.setId(1L);
        expiredBooking.setStatus(BookingStatus.CONFIRMED);
        expiredBooking.setStartTime(LocalDateTime.now().minusHours(2));
        expiredBooking.setEndTime(LocalDateTime.now().minusHours(1)); // Время уже прошло
        Pc pc = new Pc();
        expiredBooking.setPc(pc);

        when(bookingRepository.findByStatusIn(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(List.of(expiredBooking));
        when(pcStatusService.syncStatus(pc)).thenReturn(true);

        bookingService.autoCancelExpiredBookings();

        assertEquals(BookingStatus.CANCELLED, expiredBooking.getStatus());
        verify(bookingRepository).save(expiredBooking);
        verify(messagingTemplate).convertAndSend("/topic/pcs", "update");
        verify(messagingTemplate).convertAndSend("/topic/bookings", "update");
    }
}
