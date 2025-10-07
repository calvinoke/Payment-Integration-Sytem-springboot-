package com.example.pis.security;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.pis.entity.Role;
import com.example.pis.service.JwtService;

import jakarta.servlet.ServletException;

/**
 * Unit tests for JwtFilter. This class tests that the JwtFilter correctly sets
 * or does not set authentication in the SecurityContext based on JWT token
 * validity.
 */
class JwtFilterUnitTest {

    @Mock
    private JwtService jwtService;  // Mocked service to simulate JWT operations

    @InjectMocks
    private JwtFilter jwtFilter;    // The filter we are testing

    /**
     * Setup method runs before each test. Initializes Mockito mocks and clears
     * SecurityContext to avoid state leaks.
     */
    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    /**
     * Tear down method runs after each test. Clears SecurityContext to ensure
     * no leftover authentication affects other tests.
     */
    @SuppressWarnings("unused")
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Test case for a valid JWT token. Verifies that: - JwtService methods are
     * called to extract username and role. - SecurityContext is correctly
     * populated with a UsernamePasswordAuthenticationToken.
     */
    @Test
    void doFilter_ValidToken_SetsAuthentication() throws ServletException, IOException {
        // Create mock HTTP request, response, and filter chain
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // Simulate Authorization header with a valid token
        String token = "validToken";
        request.addHeader("Authorization", "Bearer " + token);

        // Stub JwtService behavior
        when(jwtService.extractUsername(token)).thenReturn("john");
        when(jwtService.extractRole(token)).thenReturn(Role.USER);
        when(jwtService.validateToken(token)).thenReturn(true);

        // Call the filter
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Verify JwtService interactions
        verify(jwtService).extractUsername(token);
        verify(jwtService).extractRole(token);

        // Verify that SecurityContext has authentication set
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set in SecurityContext");
        assertEquals("john", authentication.getPrincipal(), "Principal should match username");
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken,
                "Authentication should be of type UsernamePasswordAuthenticationToken");
    }

    /**
     * Test case for an invalid JWT token. Verifies that: - JwtService is called
     * to validate the token. - JwtService does NOT extract username or role. -
     * SecurityContext remains empty.
     */
    @Test
    void doFilter_InvalidToken_DoesNotSetAuthentication() throws ServletException, IOException {
        // Create mock HTTP request, response, and filter chain
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // Simulate Authorization header with an invalid token
        String token = "invalidToken";
        request.addHeader("Authorization", "Bearer " + token);

        // Stub JwtService behavior: token is invalid
        when(jwtService.validateToken(token)).thenReturn(false);

        // Call the filter
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Verify JwtService interactions
        verify(jwtService).validateToken(token);
        verify(jwtService, never()).extractUsername(any());
        verify(jwtService, never()).extractRole(any());

        // Verify that SecurityContext has no authentication
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication should not be set for invalid token");
    }
}
