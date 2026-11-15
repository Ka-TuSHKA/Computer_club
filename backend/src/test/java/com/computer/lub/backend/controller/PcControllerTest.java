package com.computer.lub.backend.controller;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PcControllerTest {

    @Mock private PcRepository pcRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SessionRepository sessionRepository;
    @InjectMocks private PcController pcController;

    // =====================================================================
    // ТЕСТЫ ДЛЯ ПОЛУЧЕНИЯ СПИСКА ПК (GET) - Включая Auto-Heal
    // =====================================================================

    // ПОЗИТИВНЫЙ ТЕСТ: Получение списка ПК без активных броней и сессий.
    @Test
    void shouldReturnAllPcsWithoutActiveBookings() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("FREE");

        when(pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc()).thenReturn(List.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        List<Pc> result = pcController.getAllPcs();

        assertEquals(1, result.size());
        assertEquals("FREE", result.get(0).getStatus());
        verify(pcRepository, never()).save(any(Pc.class));
    }

    // ПОЗИТИВНЫЙ ТЕСТ (Бизнес-логика): Автоматическая смена статуса на BOOKED, если наступило время брони.
    @Test
    void shouldUpdatePcStatusToBookedIfBookingIsActive() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("FREE");

        Booking activeBooking = new Booking();
        activeBooking.setStartTime(LocalDateTime.now().minusHours(1));
        activeBooking.setEndTime(LocalDateTime.now().plusHours(1));

        when(pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc()).thenReturn(List.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(1L, List.of("CONFIRMED"))).thenReturn(List.of(activeBooking));
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        List<Pc> result = pcController.getAllPcs();

        assertEquals("BOOKED", result.get(0).getStatus());
        verify(pcRepository, times(1)).save(argThat(savedPc -> "BOOKED".equals(savedPc.getStatus())));
    }

    // НОВЫЙ ТЕСТ (Auto-Heal): Если ПК висит в BOOKED, но активных сессий и броней нет, он должен стать FREE.
    @Test
    void shouldResetToFreeIfPcIsBookedButNoActiveSessionOrBooking() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("BOOKED"); // "Зависший" статус после перезагрузки сервера

        when(pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc()).thenReturn(List.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(1L, List.of("CONFIRMED"))).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        List<Pc> result = pcController.getAllPcs();

        assertEquals("FREE", result.get(0).getStatus()); // Статус сброшен
        verify(pcRepository, times(1)).save(argThat(savedPc -> "FREE".equals(savedPc.getStatus())));
    }

    // ГРАНИЧНЫЙ ТЕСТ: ПК в ремонте не трогаем, даже если есть брони.
    @Test
    void shouldNotTouchPcInRepairStatus() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("REPAIR");

        when(pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc()).thenReturn(List.of(pc));

        List<Pc> result = pcController.getAllPcs();

        assertEquals("REPAIR", result.get(0).getStatus());
        verify(pcRepository, never()).save(any(Pc.class));
    }

    // =====================================================================
    // ТЕСТЫ ДЛЯ СОЗДАНИЯ (POST)
    // =====================================================================

    @Test
    void shouldCreatePc() {
        Pc newPc = new Pc();
        newPc.setPcNumber("PC-NEW");
        newPc.setStatus("FREE");

        when(pcRepository.save(any(Pc.class))).thenReturn(newPc);

        ResponseEntity<Pc> response = pcController.createPc(newPc);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("PC-NEW", response.getBody().getPcNumber());
        verify(pcRepository, times(1)).save(newPc);
    }

    // =====================================================================
    // ТЕСТЫ ДЛЯ ОБНОВЛЕНИЯ (PUT)
    // =====================================================================

    // ПОЗИТИВНЫЙ ТЕСТ: Успешное обновление данных ПК (смена статуса на REPAIR).
    @Test
    void shouldUpdatePc() {
        Pc existingPc = new Pc();
        existingPc.setId(1L);
        existingPc.setStatus("FREE");
        existingPc.setPcNumber("OLD-NAME");

        Pc updatedDetails = new Pc();
        updatedDetails.setPcNumber("NEW-NAME");
        updatedDetails.setStatus("REPAIR");
        updatedDetails.setProcessor("Intel i9");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(existingPc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());
        when(pcRepository.save(any(Pc.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<?> response = pcController.updatePc(1L, updatedDetails);

        assertEquals(200, response.getStatusCode().value());
        Pc savedPc = (Pc) response.getBody();
        assertNotNull(savedPc);
        assertEquals("NEW-NAME", savedPc.getPcNumber());
        assertEquals("REPAIR", savedPc.getStatus());
        assertEquals("Intel i9", savedPc.getProcessor());
        verify(pcRepository, times(1)).save(any(Pc.class));
    }

    // НЕГАТИВНЫЙ ТЕСТ: Нельзя отправить в ремонт, если есть активная сессия.
    @Test
    void shouldNotUpdatePcToRepairIfHasActiveSession() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("FREE");

        Pc updatedDetails = new Pc();
        updatedDetails.setStatus("REPAIR");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(1L, List.of("PENDING", "CONFIRMED"))).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(new Session()));

        ResponseEntity<?> response = pcController.updatePc(1L, updatedDetails);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Нельзя отправить ПК в ремонт, пока на нем есть активные брони или сеансы", response.getBody());
        verify(pcRepository, never()).save(any(Pc.class));
    }

    // НЕГАТИВНЫЙ ТЕСТ: Нельзя отправить в ремонт, если есть активная бронь.
    @Test
    void shouldNotUpdatePcToRepairIfHasActiveBooking() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("FREE");

        Pc updatedDetails = new Pc();
        updatedDetails.setStatus("REPAIR");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(1L, List.of("PENDING", "CONFIRMED"))).thenReturn(List.of(new Booking()));

        ResponseEntity<?> response = pcController.updatePc(1L, updatedDetails);

        assertEquals(400, response.getStatusCode().value());
        verify(pcRepository, never()).save(any(Pc.class));
    }

    // =====================================================================
    // ТЕСТЫ ДЛЯ УДАЛЕНИЯ (DELETE)
    // =====================================================================

    // ПОЗИТИВНЫЙ ТЕСТ: Успешное удаление свободного ПК.
    @Test
    void shouldDeleteFreePc() {
        Pc freePc = new Pc();
        freePc.setId(1L);
        freePc.setStatus("FREE");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(freePc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        ResponseEntity<?> response = pcController.deletePc(1L);

        assertEquals(200, response.getStatusCode().value());
        verify(pcRepository, times(1)).delete(freePc);
    }

    // НЕГАТИВНЫЙ ТЕСТ: Нельзя удалить ПК с активной бронью.
    @Test
    void shouldNotDeletePcWithActiveBooking() {
        Pc pc = new Pc();
        pc.setId(1L);
        pc.setStatus("FREE");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of(new Booking()));

        ResponseEntity<?> response = pcController.deletePc(1L);

        assertEquals(400, response.getStatusCode().value());
        verify(pcRepository, never()).delete(any(Pc.class));
    }

    // НЕГАТИВНЫЙ ТЕСТ: Нельзя удалить занятый ПК (статус BOOKED).
    @Test
    void shouldNotDeleteBookedPc() {
        Pc bookedPc = new Pc();
        bookedPc.setId(1L);
        bookedPc.setStatus("BOOKED");

        when(pcRepository.findById(1L)).thenReturn(Optional.of(bookedPc));
        when(bookingRepository.findByPcIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(sessionRepository.findByPcIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        ResponseEntity<?> response = pcController.deletePc(1L);

        assertEquals(400, response.getStatusCode().value());
        verify(pcRepository, never()).delete(any(Pc.class));
    }
}
