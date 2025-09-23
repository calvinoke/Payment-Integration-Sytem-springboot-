package com.example.pis.security;

import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // Rollback after each test
class CustomUserDetailsServiceIntegrationTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User savedUser;

    @BeforeEach
    void setup() {
        // Save a user to the H2 test database
        User user = new User();
        user.setUsername("john");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmail("john@example.com");
        user.setRole("USER");
        savedUser = userRepository.save(user);
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("john");

        assertNotNull(userDetails);
        assertEquals("john", userDetails.getUsername());
        assertTrue(passwordEncoder.matches("password", userDetails.getPassword()));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_UserDoesNotExist_ThrowsException() {
        assertThrows(UsernameNotFoundException.class, () -> 
            userDetailsService.loadUserByUsername("nonexistent")
        );
    }
}
