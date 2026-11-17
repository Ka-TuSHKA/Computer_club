package com.computer.lub.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectRegistrationWithTooShortPassword() throws Exception {
        // Отправляем запрос с коротким паролем (3 символа, а нужно минимум 4)
        String jsonContent = "{\"username\":\"newuser\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest()); // Ожидаем 400 (ошибка валидации)
    }

    @Test
    void shouldRejectRegistrationWithTooShortUsername() throws Exception {
        // Логин из 2 символов (а нужно минимум 3)
        String jsonContent = "{\"username\":\"ab\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
    }
}
