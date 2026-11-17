package com.computer.lub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.computer.lub.backend.dto.AuthRequest;
import com.computer.lub.backend.dto.AuthResponse;
import com.computer.lub.backend.service.AuthService;
import com.computer.lub.backend.validation.OnLogin;
import com.computer.lub.backend.validation.OnRegister;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Validated(OnLogin.class) @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Validated(OnRegister.class) @RequestBody AuthRequest request) {
        return authService.register(request);
    }
}
