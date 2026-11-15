package com.computer.lub.backend.controller;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pcs")
@RequiredArgsConstructor
public class PcController {

    private final PcRepository pcRepository;
    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;

    @GetMapping
    public List<Pc> getAllPcs() {
        List<Pc> pcs = pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc();
        LocalDateTime now = LocalDateTime.now();

        for (Pc pc : pcs) {
            if ("REPAIR".equals(pc.getStatus())) continue;

            // 1. Проверяем, есть ли ПРИЧИНА быть занятым (подтвержденная бронь прямо сейчас ИЛИ активная сессия)
            boolean hasActiveSession = !sessionRepository.findByPcIdAndStatus(pc.getId(), "ACTIVE").isEmpty();

            boolean isTimeToPlay = bookingRepository.findByPcIdAndStatusIn(pc.getId(), List.of("CONFIRMED"))
                .stream().anyMatch(b -> now.isAfter(b.getStartTime()) && now.isBefore(b.getEndTime()));

            // 2. Если есть причина — ставим BOOKED
            if ((isTimeToPlay || hasActiveSession) && !"BOOKED".equals(pc.getStatus())) {
                pc.setStatus("BOOKED");
                pcRepository.save(pc);
            }
            // 3. Если причины нет, но ПК висит в BOOKED (например, после перезагрузки сервера) — освобождаем его
            else if (!isTimeToPlay && !hasActiveSession && "BOOKED".equals(pc.getStatus())) {
                pc.setStatus("FREE");
                pcRepository.save(pc);
            }
        }
        return pcs;
    }

    @PostMapping
    public ResponseEntity<Pc> createPc(@RequestBody Pc pc) {
        return ResponseEntity.ok(pcRepository.save(pc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePc(@PathVariable Long id, @RequestBody Pc pcDetails) {
        Pc pc = pcRepository.findById(id).orElseThrow();

        if ("REPAIR".equals(pcDetails.getStatus())) {
            boolean hasActiveBookings = !bookingRepository.findByPcIdAndStatusIn(id, List.of("PENDING", "CONFIRMED")).isEmpty();
            boolean hasActiveSession = !sessionRepository.findByPcIdAndStatus(id, "ACTIVE").isEmpty();

            if (hasActiveBookings || hasActiveSession) {
                return ResponseEntity.badRequest().body("Нельзя отправить ПК в ремонт, пока на нем есть активные брони или сеансы");
            }
        }

        pc.setPcNumber(pcDetails.getPcNumber());
        pc.setRowNumber(pcDetails.getRowNumber());
        pc.setSeatNumber(pcDetails.getSeatNumber());
        pc.setStatus(pcDetails.getStatus());
        pc.setProcessor(pcDetails.getProcessor());
        pc.setRam(pcDetails.getRam());
        pc.setGpu(pcDetails.getGpu());
        pc.setComment(pcDetails.getComment());

        return ResponseEntity.ok(pcRepository.save(pc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePc(@PathVariable Long id) {
        Pc pc = pcRepository.findById(id).orElseThrow();

        boolean hasActiveBookings = !bookingRepository.findByPcIdAndStatusIn(id, List.of("PENDING", "CONFIRMED")).isEmpty();
        boolean hasActiveSession = !sessionRepository.findByPcIdAndStatus(id, "ACTIVE").isEmpty();

        if ("BOOKED".equals(pc.getStatus()) || hasActiveBookings || hasActiveSession) {
            return ResponseEntity.badRequest().body("Нельзя удалить компьютер, который занят или имеет активные брони");
        }

        pcRepository.delete(pc);
        return ResponseEntity.ok().build();
    }
}
