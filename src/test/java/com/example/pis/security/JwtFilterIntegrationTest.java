package com.example.pis.security;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.pis.entity.Role;
import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;
import com.example.pis.service.JwtService;

/**
 * Integration tests for verifying JwtFilter behavior in a real Spring Boot context.
 * 
 * Tests:
 *  1. Access with a valid token (should succeed)
 *  2. Access without a token (should be denied)
 *  3. Access with an invalid token (should be denied)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtFilterIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(JwtFilterIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestRestTemplate restTemplate;

    private User testUser;
    private String token;

    /**
     * Sets up a fresh test user and generates a valid JWT before each test.
     * Ensures database and authentication state are clean.
     */
    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("john");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRole(Role.USER);
        testUser.setEmail("john@example.com");
        userRepository.save(testUser);

        // Generate a valid JWT token for authentication
        token = jwtService.generateTokenWithClaims(testUser.getUsername(), testUser.getRole());
        log.info("Setup complete: created test user '{}' with role {} and generated JWT", 
                 testUser.getUsername(), testUser.getRole());
    }

    /**
     * Builds the full URL to the protected API endpoint.
     * Uses fromUriString() (recommended in Spring 6.2+) to avoid deprecation warnings.
     */
    private String getProtectedUrl() {
        return UriComponentsBuilder
                .fromUriString("http://localhost:" + port)
                .path("/api/protected")
                .toUriString();
    }

    /**
     * Tests that a request with a valid JWT token is successfully authenticated
     * and can access the protected endpoint.
     */
    @Test
    void protectedEndpoint_WithValidToken_AllowsAccess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                getProtectedUrl(),
                HttpMethod.GET,
                entity,
                String.class
        );

        log.info("Response (valid token): status={} body={}", 
                 response.getStatusCode(), response.getBody());

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP 200 OK");

        String body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertTrue(body.contains("success"), "Expected 'success' in response body");
    }

    /**
     * Tests that accessing the endpoint without providing a token results in
     * UNAUTHORIZED (401) or FORBIDDEN (403) status.
     */
    @Test
    void protectedEndpoint_WithoutToken_DeniesAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getProtectedUrl(),
                String.class
        );

        log.warn("Response (no token): status={} body={}", 
                 response.getStatusCode(), response.getBody());

        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403 for missing token"
        );
    }

    /**
     * Tests that an invalid JWT token is rejected by the filter,
     * returning an UNAUTHORIZED (401) status.
     */
    @Test
    void protectedEndpoint_WithInvalidToken_DeniesAccess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.token.here");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                getProtectedUrl(),
                HttpMethod.GET,
                entity,
                String.class
        );

        log.error("Response (invalid token): status={} body={}", 
                  response.getStatusCode(), response.getBody());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Expected HTTP 401 Unauthorized");
    }
}
