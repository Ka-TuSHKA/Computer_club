package com.computer.lub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.computer.lub.backend.validation.OnLogin;
import com.computer.lub.backend.validation.OnRegister;

@Data
public class AuthRequest {

    @NotBlank(message = "Логин не может быть пустым", groups = {OnLogin.class, OnRegister.class})
    @Size(min = 3, max = 32, message = "Логин должен содержать от 3 до 20 символов", groups = OnRegister.class)
    private String username;

    @NotBlank(message = "Пароль не может быть пустым", groups = {OnLogin.class, OnRegister.class})
    @Size(min = 4, max = 20, message = "Пароль должен содержать от 4 до 20 символов", groups = OnRegister.class)
    private String password;
}
