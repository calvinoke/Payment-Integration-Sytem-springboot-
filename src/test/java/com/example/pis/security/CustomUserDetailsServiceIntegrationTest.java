package com.example.pis.security;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.pis.entity.Role;
import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;

/**
 * Integration tests for CustomUserDetailsService.
 * Validates that existing users can be loaded successfully,
 * and non-existing users trigger a UsernameNotFoundException.
 */
@SpringBootTest
@Transactional  // Ensures DB rollback after each test
class CustomUserDetailsServiceIntegrationTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SuppressWarnings("unused")
    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        saveUser("john", "password", Role.USER);
        saveUser("admin", "adminpass", Role.ADMIN);
    }

    private void saveUser(String username, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(username + "@example.com");
        user.setRole(role);
        userRepository.save(user);
    }

    /** Provides valid users for parameterized test. */
    @SuppressWarnings("unused") // Used reflectively by JUnit
    static Stream<TestUser> existingUsers() {
        return Stream.of(
                new TestUser("john", "password", Role.USER),
                new TestUser("admin", "adminpass", Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource("existingUsers")
    void loadUserByUsername_UserExists_ReturnsUserDetails(TestUser testUser) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.username);

        assertNotNull(userDetails, "UserDetails should not be null");
        assertEquals(testUser.username, userDetails.getUsername(), "Username should match");
        assertTrue(passwordEncoder.matches(testUser.password, userDetails.getPassword()),
                "Encoded password should match original");
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + testUser.role.name())),
                "Authorities should contain expected role");
    }

    @ParameterizedTest
    @MethodSource("nonExistingUsers")
    void loadUserByUsername_UserDoesNotExist_ThrowsException(String username) {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(username),
                "Expected UsernameNotFoundException for non-existent user"
        );

        // Check that message includes username (or your service’s exact message)
        assertTrue(
                exception.getMessage().toLowerCase().contains(username.toLowerCase()) ||
                exception.getMessage().toLowerCase().contains("not found"),
                "Exception message should mention missing username or 'not found'"
        );
    }

    /** Provides invalid usernames for parameterized test. */
    @SuppressWarnings("unused") // Used reflectively by JUnit
    static Stream<String> nonExistingUsers() {
        return Stream.of("ghost", "nobody", "unknown");
    }

    /** Helper record to hold test user data. */
    static class TestUser {
        final String username;
        final String password;
        final Role role;

        TestUser(String username, String password, Role role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}
