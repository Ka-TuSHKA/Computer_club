package com.computer.lub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.computer.lub.backend.dto.AuthRequest;
import com.computer.lub.backend.dto.AuthResponse;
import com.computer.lub.backend.entity.Role;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.UserRepository;
import com.computer.lub.backend.security.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private AuthRequest request(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void shouldLoginSuccessfully() {
        TestingAuthenticationToken mockAuth = new TestingAuthenticationToken("admin", "admin", "ROLE_ADMIN");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtUtils.generateToken("admin")).thenReturn("fake-jwt");

        AuthResponse response = authService.login(request("admin", "admin"));

        assertEquals("fake-jwt", response.getToken());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void shouldThrowOnInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Неверный логин или пароль"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request("admin", "wrong")));
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void shouldRegisterNewUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        ResponseEntity<String> response = authService.register(request("newuser", "password123"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Пользователь успешно зарегистрирован", response.getBody());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newuser", captor.getValue().getUsername());
        assertEquals("hashed-password", captor.getValue().getPassword());
        assertEquals(Role.USER, captor.getValue().getRole());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        ResponseEntity<String> response = authService.register(request("admin", "password123"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Пользователь с таким логином уже существует", response.getBody());
        verify(userRepository, never()).save(any(User.class));
    }
}
