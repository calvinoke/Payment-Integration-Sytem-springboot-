package com.example.pis.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.dto.UserResponseDTO;
import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;

/**
 * Service for user management: registration, login validation, and retrieval.
 */
@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with username, password, email, and role.
     * Defaults to role "USER" if none provided.
     *
     * @param dto User registration DTO
     * @return UserResponseDTO with registration result
     * @throws RuntimeException if username already exists
     */
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        if (repo.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String role = (dto.getRole() == null || dto.getRole().isBlank()) ? "USER" : dto.getRole().toUpperCase();

        User user = new User(dto.getUsername(), encodedPassword, dto.getEmail(), role);
        repo.save(user);

        return new UserResponseDTO("User registered successfully", user.getUsername(), user.getRole());
    }

    /**
     * Validates user credentials for login.
     *
     * @param username Raw username
     * @param rawPassword Raw password
     * @return true if credentials match, false otherwise
     */
    public boolean validateCredentials(String username, String rawPassword) {
        return repo.findByUsername(username)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }

    /**
     * Retrieves a user by username.
     *
     * @param username Username
     * @return User entity, or null if not found
     */
    public User getUserByUsername(String username) {
        return repo.findByUsername(username).orElse(null);
    }
}
