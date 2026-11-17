package com.computer.lub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.SessionStatus;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByOrderByStartTimeDesc();

    List<Session> findByUserUsernameOrderByStartTimeDesc(String username);

    List<Session> findByPcIdAndStatus(Long pcId, SessionStatus status);

    List<Session> findByStatus(SessionStatus status);

    boolean existsByPcIdAndStatus(Long pcId, SessionStatus status);

    boolean existsByBookingIdAndStatus(Long bookingId, SessionStatus status);

    // Есть ли у ПК сеансы вообще (включая историю) - для запрета удаления
    boolean existsByPcId(Long pcId);
}
