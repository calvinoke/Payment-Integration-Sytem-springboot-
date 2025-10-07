package com.example.pis.dto;

import com.example.pis.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration and login requests.
 * Example JSON:
 * {
 *   "username": "john_doe",
 *   "password": "Secret123!",
 *   "email": "john@example.com",
 *   "role": "USER"
 * }
 */
public class UserRequestDTO {

    /** Unique username for the user */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    /** Password for the account */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8–100 characters")
    private String password;

    /** User email */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /** Role of the user, defaults to USER if not provided */
    private Role role = Role.USER;

    public UserRequestDTO() {}

    public UserRequestDTO(String username, String password, String email, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = (role == null) ? Role.USER : role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) {
        this.role = (role == null) ? Role.USER : role;
    }

    @Override
    public String toString() {
        return "UserRequestDTO{" +
               "username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", role=" + role +
               '}';
        // password intentionally omitted
    }
}
