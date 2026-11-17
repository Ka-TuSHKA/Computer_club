package com.computer.lub.backend.service;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.SessionStatus;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcStatusServiceTest {

    @Mock private PcRepository pcRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SessionRepository sessionRepository;

    @InjectMocks
    private PcStatusService pcStatusService;

    @Test
    void shouldReturnRepairIfPcInRepair() {
        Pc pc = new Pc();
        pc.setStatus(PcStatus.REPAIR);
        assertEquals(PcStatus.REPAIR, pcStatusService.computeStatus(pc));
    }

    @Test
    void shouldReturnInUseIfActiveSessionExists() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus(PcStatus.FREE);
        when(sessionRepository.existsByPcIdAndStatus(eq(1L), eq(SessionStatus.ACTIVE))).thenReturn(true);
        assertEquals(PcStatus.IN_USE, pcStatusService.computeStatus(pc));
    }

    @Test
    void shouldReturnBookedIfCurrentBookingExists() {
        Pc pc = new Pc();
        pc.setId(1L); // ВАЖНО: задаем ID
        pc.setStatus(PcStatus.FREE);

        when(sessionRepository.existsByPcIdAndStatus(eq(1L), eq(SessionStatus.ACTIVE))).thenReturn(false);

        Booking activeBooking = new Booking();
        activeBooking.setStartTime(LocalDateTime.now().minusMinutes(10));
        activeBooking.setEndTime(LocalDateTime.now().plusMinutes(50));

        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(activeBooking));

        assertEquals(PcStatus.BOOKED, pcStatusService.computeStatus(pc));
    }

    @Test
    void shouldReturnFreeIfNoActiveSessionsOrCurrentBookings() {
        Pc pc = new Pc();
        pc.setId(1L); // ВАЖНО: задаем ID
        pc.setStatus(PcStatus.FREE);

        when(sessionRepository.existsByPcIdAndStatus(eq(1L), eq(SessionStatus.ACTIVE))).thenReturn(false);
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(Collections.emptyList());

        assertEquals(PcStatus.FREE, pcStatusService.computeStatus(pc));
    }
}
