package com.computer.lub.backend.controller;

import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.repository.PcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pcs")
@RequiredArgsConstructor
public class PcController {

    private final PcRepository pcRepository;

    @GetMapping
    public List<Pc> getAllPcs() {
        return pcRepository.findAllByOrderByRowNumberAscSeatNumberAsc();
    }
}
