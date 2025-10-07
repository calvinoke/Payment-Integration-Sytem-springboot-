package com.example.pis.security;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.pis.entity.Role;
import com.example.pis.service.JwtService;

/**
 * Unit tests for JwtService.
 * 
 * Ensures correct behavior for:
 *  - Token generation (with and without claims)
 *  - Claim extraction
 *  - Token validation (valid, invalid, expired, tampered)
 */
class JwtUtilTest{

    private JwtService jwtService;
    private String base64Secret;

    /**
     * Sets up a deterministic, Base64-encoded 256-bit key and initializes JwtService
     * before each test. This ensures a consistent signing key for reliable results.
     */
    @SuppressWarnings("unused") // IDE might think JUnit 5 doesn't use this
    @BeforeEach
    void setUp() {
        // Generate a reproducible 256-bit test key (32 bytes)
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) i;
        }
        base64Secret = Base64.getEncoder().encodeToString(keyBytes);

        jwtService = new JwtService(
                base64Secret,
                3600000L,      // 1 hour expiration
                "test-issuer",
                "test-audience"
        );

        assertNotNull(jwtService, "JwtService should be initialized in setUp()");
    }

    /**
     * Tests that a token can be generated and that the username embedded inside
     * can be extracted successfully.
     */
    @Test
    @DisplayName("Generate and extract username from token")
    void generateToken_and_extractUsername_shouldWork() {
        String username = "testuser";
        String token = jwtService.generateToken(username);

        assertNotNull(token, "Token should not be null");
        assertEquals(username, jwtService.extractUsername(token),
                "Extracted username should match original");
    }

    /**
     * Verifies that a token generated with additional claims (like role)
     * can correctly return both username and role.
     */
    @Test
    @DisplayName("Generate token with role claim and extract both username and role")
    void generateTokenWithClaims_and_extractRole_shouldWork() {
        String username = "adminuser";
        Role role = Role.ADMIN;

        String token = jwtService.generateTokenWithClaims(username, role);

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));
        assertEquals(role, jwtService.extractRole(token));
    }

    /**
     * Confirms that a freshly created token is recognized as valid.
     */
    @Test
    @DisplayName("Validate a properly generated token")
    void validateToken_withValidToken_shouldReturnTrue() {
        String username = "validuser";
        String token = jwtService.generateToken(username);

        assertTrue(jwtService.validateToken(token),
                "Token should be valid immediately after generation");
    }

    /**
     * Ensures an obviously malformed token is rejected by the validator.
     */
    @Test
    @DisplayName("Reject an invalid or malformed token")
    void validateToken_withInvalidToken_shouldReturnFalse() {
        String invalidToken = "some.invalid.token";
        assertFalse(jwtService.validateToken(invalidToken),
                "Invalid token should not be accepted");
    }

    /**
     * Tests that an expired token is properly detected and rejected.
     * The test uses a very short expiration time (1 ms).
     */

    @Test
    @DisplayName("Reject an expired token")
    void validateToken_withExpiredToken_shouldReturnFalse() throws InterruptedException {
        // Create a JwtService with 1ms expiration for testing expiration
        JwtService shortLivedService = new JwtService(
                base64Secret,
                1L,                  // expires almost instantly
                "test-issuer",
                "test-audience"
        );

        String token = shortLivedService.generateToken("shortliveduser");

        // Wait briefly to ensure token has expired
        TimeUnit.MILLISECONDS.sleep(10);

        assertFalse(shortLivedService.validateToken(token),
                "Expired token should not be valid");
    }

    /**
     * Ensures that tampering with a valid token (e.g., changing one character)
     * causes signature verification to fail.
     */
    @Test
    @DisplayName("Reject a tampered token (signature mismatch)")
    void validateToken_withTamperedToken_shouldReturnFalse() {
        String username = "tampertest";
        String validToken = jwtService.generateToken(username);

        // Modify a single character in the token to simulate tampering
        String tamperedToken = validToken.substring(0, validToken.length() - 2) + "xx";

        assertFalse(jwtService.validateToken(tamperedToken),
                "Tampered token should fail signature validation");
    }
}
