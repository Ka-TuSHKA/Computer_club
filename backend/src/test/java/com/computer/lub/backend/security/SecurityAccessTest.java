package com.computer.lub.backend.security;

import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PcRepository pcRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    // НЕГАТИВНЫЙ: Анонимный пользователь не может получить список ПК.
    @Test
    void shouldDenyAccessForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/pcs"))
                .andExpect(status().isForbidden());
    }

    // НЕГАТИВНЫЙ: Невалидный JWT-токен не дает доступ.
    @Test
    void shouldDenyAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/pcs")
                .header("Authorization", "Bearer invalid.fake.token"))
                .andExpect(status().isForbidden());
    }

    // НЕГАТИВНЫЙ: USER не может создать ПК.
    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyCreatePcForUser() throws Exception {
        mockMvc.perform(post("/api/pcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pcNumber\":\"PC-HACK\",\"status\":\"FREE\",\"rowNumber\":1,\"seatNumber\":1}"))
                .andExpect(status().isForbidden());
    }

    // НЕГАТИВНЫЙ: USER не может удалить ПК.
    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyDeletePcForUser() throws Exception {
        mockMvc.perform(delete("/api/pcs/1"))
                .andExpect(status().isForbidden());
    }

    // НЕГАТИВНЫЙ: USER не может подтвердить бронь.
    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyConfirmBookingForUser() throws Exception {
        mockMvc.perform(put("/api/bookings/1/confirm"))
                .andExpect(status().isForbidden());
    }

    // ПОЗИТИВНЫЙ: ADMIN может создать ПК.
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowCreatePcForAdmin() throws Exception {
        Pc mockPc = new Pc();
        mockPc.setId(1L);
        mockPc.setPcNumber("PC-NEW");
        mockPc.setStatus(PcStatus.FREE);

        when(pcRepository.save(any(Pc.class))).thenReturn(mockPc);

        // Передаем все обязательные поля, чтобы пройти валидацию
        String jsonContent = "{\"pcNumber\":\"PC-NEW\",\"status\":\"FREE\",\"rowNumber\":1,\"seatNumber\":1}";

        mockMvc.perform(post("/api/pcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isOk());
    }

    // ПОЗИТИВНЫЙ: USER может просматривать карту (GET /api/pcs).
    @Test
    @WithMockUser(roles = "USER")
    void shouldAllowGetPcsForUser() throws Exception {
        when(pcRepository.findAllByDeletedFalseOrderByRowNumberAscSeatNumberAsc()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/pcs"))
                .andExpect(status().isOk());
    }
}
