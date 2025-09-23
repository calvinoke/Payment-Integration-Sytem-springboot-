package com.example.pis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.User;
import com.example.pis.service.JwtService;
import com.example.pis.service.UserService;

import jakarta.validation.Valid;

/**
 * REST controller for authentication endpoints: register and login.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000") // Update for production frontend URL
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        try {
            UserResponseDTO response = userService.registerUser(dto);
            // token is null in this flow
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new UserResponseDTO(e.getMessage(), dto.getUsername(), "USER"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDTO("Registration failed", dto.getUsername(), "USER"));
        }
    }

    /**
     * Authenticate a user and return JWT along with role.
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody UserRequestDTO dto) {
        try {
            boolean valid = userService.validateCredentials(dto.getUsername(), dto.getPassword());
            if (!valid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserResponseDTO("Invalid username or password",
                                                  dto.getUsername(), "USER"));
            }

            User user = userService.getUserByUsername(dto.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserResponseDTO("User not found",
                                                  dto.getUsername(), "USER"));
            }

            // Generate JWT token
            String token = jwtService.generateToken(user.getUsername());

            // Return response including token
            return ResponseEntity.ok(
                new UserResponseDTO("Login successful",
                                    user.getUsername(),
                                    user.getRole(),
                                    token)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDTO("Login failed", dto.getUsername(), "USER"));
        }
    }
}
