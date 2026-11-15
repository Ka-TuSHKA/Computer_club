package com.computer.lub.backend.repository;

import com.computer.lub.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByStartTimeDesc();
    List<Booking> findByUserUsernameOrderByStartTimeDesc(String username);

    // Метод для получения активных броней конкретного ПК
    List<Booking> findByPcIdAndStatusIn(Long pcId, List<String> statuses);
    List<Booking> findByStatus(String status);
}
