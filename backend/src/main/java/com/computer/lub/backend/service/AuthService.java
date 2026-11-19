package com.computer.lub.backend.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.computer.lub.backend.dto.AuthRequest;
import com.computer.lub.backend.dto.AuthResponse;
import com.computer.lub.backend.entity.Role;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.event.AuditEvent;
import com.computer.lub.backend.repository.UserRepository;
import com.computer.lub.backend.security.JwtUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public AuthResponse login(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtUtils.generateToken(auth.getName());
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        eventPublisher.publishEvent(new AuditEvent(this, "LOGIN", auth.getName(), "role=" + role));

        return new AuthResponse(auth.getName(), role, token);
    }

    public ResponseEntity<String> register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Пользователь с таким логином уже существует");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        eventPublisher.publishEvent(new AuditEvent(this, "REGISTER", request.getUsername(), "role=USER"));

        return ResponseEntity.ok("Пользователь успешно зарегистрирован");
    }
}
