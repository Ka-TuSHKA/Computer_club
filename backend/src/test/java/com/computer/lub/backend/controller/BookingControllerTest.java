package com.computer.lub.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectBookingWithoutPcId() throws Exception {
        // pcId = null
        String jsonContent = "{\"date\":\"2030-01-01\",\"startTime\":\"12:00\",\"durationMinutes\":60}";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectBookingWithoutDate() throws Exception {
        // date = null
        String jsonContent = "{\"pcId\":1,\"startTime\":\"12:00\",\"durationMinutes\":60}";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectBookingWithoutStartTime() throws Exception {
        // startTime = null
        String jsonContent = "{\"pcId\":1,\"date\":\"2030-01-01\",\"durationMinutes\":60}";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectBookingWithTooShortDuration() throws Exception {
        // durationMinutes = 10 (меньше минимума 30)
        String jsonContent = "{\"pcId\":1,\"date\":\"2030-01-01\",\"startTime\":\"12:00\",\"durationMinutes\":10}";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }
}
