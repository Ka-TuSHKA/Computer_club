package com.computer.lub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.dto.WalkInSessionRequest;
import com.computer.lub.backend.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getSessions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(sessionService.getSessions(username, isAdmin));
    }

    @PostMapping("/start/{bookingId}")
    public ResponseEntity<Void> startSession(@PathVariable Long bookingId) {
        sessionService.startSession(bookingId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/walk-in")
    public ResponseEntity<Void> startWalkInSession(@Valid @RequestBody WalkInSessionRequest request) {
        sessionService.startWalkInSession(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/finish")
    public ResponseEntity<Void> finishSession(@PathVariable Long id) {
        sessionService.finishSession(id);
        return ResponseEntity.ok().build();
    }
}
