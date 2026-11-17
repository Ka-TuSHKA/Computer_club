package com.computer.lub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequest {

    @NotNull(message = "Не выбран компьютер")
    private Long pcId;

    @NotNull(message = "Не указана дата")
    private LocalDate date;

    @NotNull(message = "Не указано время начала")
    private LocalTime startTime;

    @NotNull(message = "Не указана длительность")
    @Min(value = 30, message = "Длительность должна быть не менее 30 минут")
    private Integer durationMinutes;
}
