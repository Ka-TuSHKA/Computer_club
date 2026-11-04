package com.computer.lub.backend.repository;

import com.computer.lub.backend.entity.Pc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PcRepository extends JpaRepository<Pc, Long> {
    // Сортируем по ряду, а затем по месту
    List<Pc> findAllByOrderByRowNumberAscSeatNumberAsc();
}
