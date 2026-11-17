package com.computer.lub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.computer.lub.backend.entity.Pc;

public interface PcRepository extends JpaRepository<Pc, Long> {

    // Удалённые ПК в карту и списки не попадают
    List<Pc> findAllByDeletedFalseOrderByRowNumberAscSeatNumberAsc();

    // обновлять, удалять и бронировать удалённый ПК нельзя
    Optional<Pc> findByIdAndDeletedFalse(Long id);
}
