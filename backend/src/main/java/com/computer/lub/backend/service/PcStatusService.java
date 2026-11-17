package com.computer.lub.backend.service;

import java.time.LocalDateTime;
import java.util.List;

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

//статус всегда вычисляется из состояния сеансов и броней.
@Service
@RequiredArgsConstructor
public class PcStatusService {

    private final PcRepository pcRepository;
    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;

    //Вычисляет корректный статус ПК:
    //REPAIR - задается администратором вручную и автоматически не пересчитывается
    //IN_USE - на ПК идет активный сеанс
    //BOOKED - активного сеанса нет, но действует бронь, интервал которой покрывает текущий момент
    //FREE - компьютер свободен (будущие брони статус не меняют, они видны через busy-slots)
    public PcStatus computeStatus(Pc pc) {
        if (pc.getStatus() == PcStatus.REPAIR) {
            return PcStatus.REPAIR;
        }
        LocalDateTime now = LocalDateTime.now();
        if (sessionRepository.existsByPcIdAndStatus(pc.getId(), SessionStatus.ACTIVE)) {
            return PcStatus.IN_USE;
        }
        boolean bookedNow = bookingRepository
                .findByPcIdAndStatusIn(pc.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .anyMatch(b -> !b.getStartTime().isAfter(now) && b.getEndTime().isAfter(now));
        return bookedNow ? PcStatus.BOOKED : PcStatus.FREE;
    }

    //Пересчитывает и при необходимости сохраняет статус ПК
    //Возвращает true, если статус изменился
    @Transactional
    public boolean syncStatus(Pc pc) {
        PcStatus target = computeStatus(pc);
        if (pc.getStatus() != target) {
            pc.setStatus(target);
            pcRepository.save(pc);
            return true;
        }
        return false;
    }
}
