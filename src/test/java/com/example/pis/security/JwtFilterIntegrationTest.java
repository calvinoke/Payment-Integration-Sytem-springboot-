package com.example.pis.security;

import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort; // <-- Updated for Spring Boot 3
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtFilterIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TestRestTemplate restTemplate;

    private User testUser;
    private String token;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("john");
        testUser.setPassword("password"); // Ideally, encode this if using Spring Security
        testUser.setRole("USER");
        testUser.setEmail("john@example.com");
        userRepository.save(testUser);

        token = jwtUtil.generateToken(testUser.getUsername());
    }

    @Test
    void protectedEndpoint_WithValidToken_AllowsAccess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/protected",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_WithoutToken_DeniesAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/protected",
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
