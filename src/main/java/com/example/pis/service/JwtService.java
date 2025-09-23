package com.example.pis.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service for generating JSON Web Tokens (JWT) for authentication.
 */
@Service
public class JwtService {

    // Use a secure secret key (should be at least 256-bit)
    private final String secretKey = "my-super-secret-key-that-is-very-long-1234567890";

    /**
     * Generates a JWT token containing the username as subject.
     * Token expires in 24 hours.
     *
     * @param username the username to include in the token
     * @return signed JWT token as a string
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();
    }
}
