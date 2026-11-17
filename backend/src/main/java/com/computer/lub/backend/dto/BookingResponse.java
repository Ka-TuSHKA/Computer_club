package com.computer.lub.backend.dto;

import java.time.LocalDateTime;

import com.computer.lub.backend.entity.BookingStatus;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private String pcNumber;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
}
