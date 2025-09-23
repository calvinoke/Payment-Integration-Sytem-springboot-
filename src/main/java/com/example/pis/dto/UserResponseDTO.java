package com.example.pis.dto;

import java.io.Serializable;

/**
 * DTO returned after user registration or login.
 * For login, `token` will contain the JWT.
 */
public final class UserResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String message;   // Info or status message
    private final String username;  // Username
    private final String role;      // User role
    private final String token;     // JWT token (null for register)

    /**
     * Full constructor.
     */
    public UserResponseDTO(String message, String username, String role, String token) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        this.message = message;
        this.username = username;
        this.role = role;
        this.token = token; // may be null if not a login response
    }

    /** Convenience constructor for registration (no token) */
    public UserResponseDTO(String message, String username, String role) {
        this(message, username, role, null);
    }

    public String getMessage()  { return message; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getToken()    { return token; }

    @Override
    public String toString() {
        return "UserResponseDTO{" +
                "message='" + message + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
