package com.computer.lub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.service.PcService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pcs")
@RequiredArgsConstructor
public class PcController {

    private final PcService pcService;

    @GetMapping
    public List<Pc> getAllPcs() {
        return pcService.getAllPcs();
    }

    @PostMapping
    public ResponseEntity<Pc> createPc(@Valid @RequestBody Pc pc) {
        return ResponseEntity.ok(pcService.createPc(pc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePc(@PathVariable Long id, @Valid @RequestBody Pc pcDetails) {
        return pcService.updatePc(id, pcDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePc(@PathVariable Long id) {
        return pcService.deletePc(id);
    }
}
