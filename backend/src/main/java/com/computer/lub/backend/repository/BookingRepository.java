package com.computer.lub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByStartTimeDesc();

    List<Booking> findByUserUsernameOrderByStartTimeDesc(String username);

    // Активные (не отмененные и не завершенные) брони конкретного ПК
    List<Booking> findByPcIdAndStatusIn(Long pcId, List<BookingStatus> statuses);

    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    // Есть ли у ПК брони вообще (включая историю) - для запрета удаления
    boolean existsByPcId(Long pcId);
}
