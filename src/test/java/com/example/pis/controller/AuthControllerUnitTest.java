package com.example.pis.controller;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.User;
import com.example.pis.service.JwtService;
import com.example.pis.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerUnitTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterSuccess() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password");

        UserResponseDTO responseDTO = new UserResponseDTO("token", "testuser", "USER");
        when(userService.registerUser(dto)).thenReturn(responseDTO);

        ResponseEntity<UserResponseDTO> response = authController.register(dto);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("token", response.getBody().getToken());
        assertEquals("USER", response.getBody().getRole());
    }

    @Test
    void testLoginSuccess() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password");

        User user = new User();
        user.setUsername("testuser");
        user.setRole("USER");

        when(userService.validateCredentials("testuser", "password")).thenReturn(true);
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(jwtService.generateToken("testuser")).thenReturn("jwt-token");

        ResponseEntity<UserResponseDTO> response = authController.login(dto);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().getToken());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("USER", response.getBody().getRole());
    }

    @Test
    void testLoginUnauthorized() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("wrong");
        dto.setPassword("wrong");

        when(userService.validateCredentials("wrong", "wrong")).thenReturn(false);

        ResponseEntity<UserResponseDTO> response = authController.login(dto);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
