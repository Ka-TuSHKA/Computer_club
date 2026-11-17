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
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectWalkInSessionWithoutPcId() throws Exception {
        // pcId = null
        String jsonContent = "{\"username\":\"client\",\"duration\":60}";

        mockMvc.perform(post("/api/sessions/walk-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectWalkInSessionWithoutUsername() throws Exception {
        // username пустой
        String jsonContent = "{\"pcId\":1,\"username\":\"\",\"duration\":60}";

        mockMvc.perform(post("/api/sessions/walk-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectWalkInSessionWithTooShortDuration() throws Exception {
        // duration = 10 (меньше минимума 30)
        String jsonContent = "{\"pcId\":1,\"username\":\"client\",\"duration\":10}";

        mockMvc.perform(post("/api/sessions/walk-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }
}
