package com.computer.lub.backend.controller;

import com.computer.lub.backend.dto.AuthRequest;
import com.computer.lub.backend.dto.AuthResponse;
import com.computer.lub.backend.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @InjectMocks private AuthController authController;

    @Test
    void shouldLoginSuccessfully() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("admin");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(mockAuth).getAuthorities();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtUtils.generateToken("admin")).thenReturn("fake-jwt");

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("fake-jwt", response.getBody().getToken());
        assertEquals("ADMIN", response.getBody().getRole());
    }
}
