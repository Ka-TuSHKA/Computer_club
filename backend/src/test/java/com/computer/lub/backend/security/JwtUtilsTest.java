package com.computer.lub.backend.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
    private final JwtUtils jwtUtils = new JwtUtils();

    @Test
    void shouldGenerateAndExtractUsername() {
        String token = jwtUtils.generateToken("client");
        assertNotNull(token);
        assertEquals("client", jwtUtils.getUsernameFromToken(token));
    }
}
