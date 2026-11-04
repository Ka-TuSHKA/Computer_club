package com.computer.lub.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequest {
    private Long pcId;
    private LocalDate date;
    private LocalTime startTime;
    private Integer durationMinutes;
}
