package com.computer.lub.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionResponse {
    private Long id;
    private String pcNumber;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
