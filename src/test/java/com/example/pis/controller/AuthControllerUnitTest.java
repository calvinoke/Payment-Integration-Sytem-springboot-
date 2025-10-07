package com.example.pis.controller;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.Role;
import com.example.pis.entity.User;
import com.example.pis.service.UserService;
//import com.example.pis.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerUnitTest {

    @Mock
    private UserService userService;

    @Mock
   // private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterSuccess() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password");
        dto.setEmail("testuser@example.com");

        UserResponseDTO responseDTO = new UserResponseDTO(
                "User registered successfully",
                "testuser",
                Role.USER,
                null
        );

        when(userService.registerUser(dto)).thenReturn(responseDTO);

        ResponseEntity<UserResponseDTO> response = authController.register(dto);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        UserResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("testuser", body.getUsername());
        assertEquals(Role.USER, body.getRole());
        assertNull(body.getToken());
    }

    @Test
    void testLoginSuccess() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password");

        User user = new User();
        user.setUsername("testuser");
        user.setRole(Role.USER);

        when(userService.authenticateAndGenerateToken("testuser", "password")).thenReturn("jwt-token");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

        ResponseEntity<UserResponseDTO> response = authController.login(dto);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("jwt-token", body.getToken());
        assertEquals("testuser", body.getUsername());
        assertEquals(Role.USER, body.getRole());
    }

    @Test
    void testLoginUnauthorized() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("wronguser");
        dto.setPassword("wrongpassword");

        when(userService.authenticateAndGenerateToken("wronguser", "wrongpassword")).thenReturn(null);

        ResponseEntity<UserResponseDTO> response = authController.login(dto);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        UserResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid username or password", body.getMessage());
        assertEquals("wronguser", body.getUsername());
        assertEquals(Role.USER, body.getRole());
        assertNull(body.getToken());
    }

    @Test
    void testLoginUserNotFoundAfterAuth() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password");

        when(userService.authenticateAndGenerateToken("testuser", "password")).thenReturn("jwt-token");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.empty());

        ResponseEntity<UserResponseDTO> response = authController.login(dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        UserResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("User not found after login", body.getMessage());
        assertEquals("testuser", body.getUsername());
        assertEquals(Role.USER, body.getRole());
        assertNull(body.getToken());
    }
}
