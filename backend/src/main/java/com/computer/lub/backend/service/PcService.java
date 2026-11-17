package com.computer.lub.backend.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.computer.lub.backend.entity.BookingStatus;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.SessionStatus;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PcService {

    private final PcRepository pcRepository;
    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final PcStatusService pcStatusService;

    public List<Pc> getAllPcs() {
        return pcRepository.findAllByDeletedFalseOrderByRowNumberAscSeatNumberAsc();
    }

    public Pc createPc(Pc pc) {
        return pcRepository.save(pc);
    }

    @Transactional
    public ResponseEntity<?> updatePc(Long id, Pc pcDetails) {
        Pc pc = pcRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Компьютер не найден"));

        boolean hasActiveBookings = !bookingRepository
                .findByPcIdAndStatusIn(id, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)).isEmpty();
        boolean hasActiveSession = sessionRepository.existsByPcIdAndStatus(id, SessionStatus.ACTIVE);

        if (pcDetails.getStatus() == PcStatus.REPAIR && (hasActiveBookings || hasActiveSession)) {
            return ResponseEntity.badRequest()
                    .body("Нельзя отправить ПК в ремонт, пока на нем есть активные брони или сеансы");
        }

        pc.setPcNumber(pcDetails.getPcNumber());
        pc.setRowNumber(pcDetails.getRowNumber());
        pc.setSeatNumber(pcDetails.getSeatNumber());
        pc.setProcessor(pcDetails.getProcessor());
        pc.setRam(pcDetails.getRam());
        pc.setGpu(pcDetails.getGpu());
        pc.setComment(pcDetails.getComment());

        if (pcDetails.getStatus() == PcStatus.REPAIR) {
            pc.setStatus(PcStatus.REPAIR);
        } else {
            pcStatusService.syncStatus(pc);
        }

        return ResponseEntity.ok(pcRepository.save(pc));
    }

    @Transactional
    public ResponseEntity<?> deletePc(Long id) {
        Pc pc = pcRepository.findById(id).orElseThrow();

        if (bookingRepository.existsByPcId(id) || sessionRepository.existsByPcId(id)) {
            return ResponseEntity.badRequest()
                    .body("Нельзя удалить компьютер: с ним связаны брони или сеансы");
        }

        pcRepository.delete(pc);
        return ResponseEntity.ok().build();
    }
}
