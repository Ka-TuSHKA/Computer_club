package com.computer.lub.backend.service;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PcServiceTest {

    @Mock private PcRepository pcRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PcStatusService pcStatusService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private PcService pcService;

    @Test
    void shouldUpdatePcToRepair() {
        Pc existingPc = new Pc();
        existingPc.setId(1L);
        existingPc.setStatus(PcStatus.FREE);
        existingPc.setPcNumber("OLD-NAME");

        Pc updatedDetails = new Pc();
        updatedDetails.setPcNumber("NEW-NAME");
        updatedDetails.setStatus(PcStatus.REPAIR);
        updatedDetails.setProcessor("Intel i9");

        when(pcRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingPc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(sessionRepository.existsByPcIdAndStatus(1L, SessionStatus.ACTIVE)).thenReturn(false);
        when(pcRepository.save(any(Pc.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<?> response = pcService.updatePc(1L, updatedDetails);

        assertEquals(200, response.getStatusCode().value());
        Pc savedPc = (Pc) response.getBody();
        assertNotNull(savedPc);
        assertEquals("NEW-NAME", savedPc.getPcNumber());
        assertEquals(PcStatus.REPAIR, savedPc.getStatus());
        assertEquals("Intel i9", savedPc.getProcessor());
        verify(pcRepository, times(1)).save(any(Pc.class));
    }

    @Test
    void shouldNotUpdatePcToRepairIfHasActiveSession() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus(PcStatus.FREE);

        Pc updatedDetails = new Pc();
        updatedDetails.setStatus(PcStatus.REPAIR);

        when(pcRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(1L, List.of(BookingStatus.PENDING,
             BookingStatus.CONFIRMED))).thenReturn(List.of());
        when(sessionRepository.existsByPcIdAndStatus(1L, SessionStatus.ACTIVE)).thenReturn(true);

        ResponseEntity<?> response = pcService.updatePc(1L, updatedDetails);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя отправить ПК в ремонт, пока на нем есть активные брони или сеансы", response.getBody());
        verify(pcRepository, never()).save(any(Pc.class));
    }

    @Test
    void shouldSoftDeletePcWithoutActiveHistory() {
        Pc freePc = new Pc();
        freePc.setId(1L);
        freePc.setStatus(PcStatus.FREE);
        freePc.setDeleted(false); // Изначально не удален

        when(pcRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(freePc));
        // Нет активных броней
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        // Нет активных сеансов
        when(sessionRepository.existsByPcIdAndStatus(1L, SessionStatus.ACTIVE)).thenReturn(false);

        ResponseEntity<?> response = pcService.deletePc(1L);

        assertEquals(200, response.getStatusCode().value());

        // Проверяем, что флаг мягкого удаления установлен в true
        assertTrue(freePc.getDeleted());
        // Проверяем, что мы ВЫЗВАЛИ save, а не delete
        verify(pcRepository, times(1)).save(freePc);
        verify(pcRepository, never()).delete(any(Pc.class));
    }

    @Test
    void shouldNotDeletePcWithActiveHistory() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setDeleted(false);

        when(pcRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(pc));

        // Имитируем наличие активной брони (например, CONFIRMED)
        Booking activeBooking = new Booking();
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(activeBooking));

        ResponseEntity<?> response = pcService.deletePc(1L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя удалить компьютер: на нем есть активные брони или сеансы", response.getBody());

        // Проверяем, что мы НЕ вызывали save (ничего не изменилось)
        verify(pcRepository, never()).save(any(Pc.class));
    }
}
