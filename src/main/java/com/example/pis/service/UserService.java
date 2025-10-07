package com.example.pis.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.Role;
import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;

/**
 * Service layer for managing users:
 * - Registration with secure password hashing
 * - Credential validation
 * - JWT token generation with role claims
 * - Safe user lookups
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user with a unique username.
     * Encodes password securely and assigns a role (defaults to USER).
     */
    @Transactional
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        String username = dto.getUsername().trim();

        // Enforce unique username
        repo.findByUsername(username).ifPresent(user -> {
            log.warn("Registration attempt with existing username: {}", username);
            throw new IllegalStateException("Username already exists");
        });

        // Encode password
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // Determine role (default USER)
        Role role = (dto.getRole() == null) ? Role.USER : dto.getRole();

        // Create and save user
        User user = new User(username, encodedPassword, dto.getEmail(), role);

        try {
            repo.save(user);
            log.info("User registered successfully: {} (role: {})", username, role);
        } catch (DataAccessException ex) {
            log.error("Database error during registration for user {}", username, ex);
            throw new IllegalStateException("Registration failed, please retry later");
        }

        return new UserResponseDTO(
                "User registered successfully",
                user.getUsername(),
                user.getRole()
        );
    }

    /**
     * Validates username and password against stored hash.
     */
    public boolean validateCredentials(String username, String rawPassword) {
        return repo.findByUsername(username.trim())
                   .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                   .orElse(false);
    }

    /**
     * Authenticates user and returns a signed JWT containing username + role.
     * Returns null if authentication fails.
     */
    public String authenticateAndGenerateToken(String username, String rawPassword) {
        return repo.findByUsername(username.trim())
                   .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                   .map(user -> jwtService.generateTokenWithClaims(
                           user.getUsername(),
                           user.getRole() // directly pass Role enum
                   ))
                   .orElse(null);
    }

    /**
     * Retrieves user details by username.
     * Returns Optional<User> to avoid null checks.
     */
    public Optional<User> getUserByUsername(String username) {
        return repo.findByUsername(username.trim());
    }
}
