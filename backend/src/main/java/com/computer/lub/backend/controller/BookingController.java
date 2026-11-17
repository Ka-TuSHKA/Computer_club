package com.computer.lub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.computer.lub.backend.dto.BookingRequest;
import com.computer.lub.backend.dto.BookingResponse;
import com.computer.lub.backend.dto.BusySlotResponse;
import com.computer.lub.backend.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings() {
        return ResponseEntity.ok(bookingService.getBookings(currentUsername(), isAdmin()));
    }

    @GetMapping("/pc/{pcId}/busy-slots")
    public ResponseEntity<List<BusySlotResponse>> getBusySlots(@PathVariable Long pcId) {
        return ResponseEntity.ok(bookingService.getBusySlots(pcId));
    }

    @PostMapping
    public ResponseEntity<Void> createBooking(@Valid @RequestBody BookingRequest request) {
        bookingService.create(request, currentUsername());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmBooking(@PathVariable Long id) {
        bookingService.confirm(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancel(id, currentUsername(), isAdmin());
        return ResponseEntity.ok().build();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
