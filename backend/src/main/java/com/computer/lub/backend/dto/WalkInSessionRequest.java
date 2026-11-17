package com.computer.lub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalkInSessionRequest {

    @NotNull(message = "Не выбран компьютер")
    private Long pcId;

    @NotBlank(message = "Не указан пользователь")
    private String username;

    @NotNull(message = "Не указана длительность сеанса")
    @Min(value = 30, message = "Длительность сеанса должна быть не менее 30 минут")
    private Integer duration;
}
