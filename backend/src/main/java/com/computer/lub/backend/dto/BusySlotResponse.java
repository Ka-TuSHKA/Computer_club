package com.computer.lub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BusySlotResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String username;
}
