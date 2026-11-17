package com.computer.lub.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // Импортируем SecretKey
import java.util.Date;

@Component
public class JwtUtils {
    // Используем SecretKey вместо Key
    private final SecretKey key =
    Keys.hmacShaKeyFor("superSecretKeyForPrototypeComputerClubMustBeLongEnough".getBytes());

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24 часа
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
