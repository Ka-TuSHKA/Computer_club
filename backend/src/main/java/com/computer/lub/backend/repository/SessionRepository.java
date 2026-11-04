package com.computer.lub.backend.repository;

import com.computer.lub.backend.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findAllByOrderByStartTimeDesc();
    List<Session> findByUserUsernameOrderByStartTimeDesc(String username);
}
