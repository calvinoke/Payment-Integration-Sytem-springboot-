package com.example.pis.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.Role;
import com.example.pis.entity.User;
import com.example.pis.service.UserService;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * REST controller for authentication endpoints: register and login.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${cors.allowed-origins}") // Use the same property defined in application.properties
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService,
                          @Value("${cors.allowed-origins}") String allowedOrigins) { // Match the same property
        this.userService = userService;
        System.out.println("Allowed CORS origins: " + allowedOrigins);
    }

    /** Register a new user. */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        try {
            UserResponseDTO response = userService.registerUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new UserResponseDTO(e.getMessage(), dto.getUsername(), Role.USER));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDTO("Registration failed", dto.getUsername(), Role.USER));
        }
    }

    /** Authenticate a user and return JWT along with role claim. */
    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody UserRequestDTO dto) {
        try {
            String token = userService.authenticateAndGenerateToken(dto.getUsername(), dto.getPassword());

            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserResponseDTO("Invalid username or password",
                                dto.getUsername(), Role.USER));
            }

            Optional<User> userOpt = userService.getUserByUsername(dto.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new UserResponseDTO("User not found after login", dto.getUsername(), Role.USER));
            }

            User user = userOpt.get();
            return ResponseEntity.ok(
                    new UserResponseDTO("Login successful",
                            user.getUsername(),
                            user.getRole(),
                            token)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDTO("Login failed", dto.getUsername(), Role.USER));
        }
    }
}
