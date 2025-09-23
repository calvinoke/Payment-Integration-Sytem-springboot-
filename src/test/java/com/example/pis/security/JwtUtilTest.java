package com.example.pis.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void generateToken_and_extractUsername_shouldWork() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        String username = "validuser";
        String token = jwtUtil.generateToken(username);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        String invalidToken = "some.invalid.token";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }
}
