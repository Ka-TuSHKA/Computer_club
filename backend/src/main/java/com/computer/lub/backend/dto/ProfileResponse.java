package com.computer.lub.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfileResponse {
    private String username;
    private String role;
    private List<BookingResponse> bookings;
    private List<SessionResponse> sessions;
}
