package com.example.pis.dto;

import java.io.Serializable;

import com.example.pis.entity.Role;

/**
 * DTO returned after user registration or login.
 * For login, `token` will contain the JWT.
 */
public final class UserResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String message;   // Info or status message
    private final String username;  // Username
    private final Role role;        // User role (enum)
    private final String token;     // JWT token (null for register)

    /**
     * Full constructor.
     */
    public UserResponseDTO(String message, String username, Role role, String token) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.message = message;
        this.username = username;
        this.role = role;
        this.token = token; // may be null if not a login response
    }

    /** Convenience constructor for registration (no token) */
    public UserResponseDTO(String message, String username, Role role) {
        this(message, username, role, null);
    }

    public String getMessage()  { return message; }
    public String getUsername() { return username; }
    public Role getRole()       { return role; }
    public String getToken()    { return token; }

    @Override
    public String toString() {
        return "UserResponseDTO{" +
                "message='" + message + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", token='" + token + '\'' +
                '}';
    }
}
