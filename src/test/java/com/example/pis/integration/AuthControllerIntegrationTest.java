package com.example.pis.integration;

import com.example.pis.dto.UserRequestDTO;
import com.example.pis.entity.User;
import com.example.pis.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use H2 in-memory DB for testing
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unused") // Suppress IDE warning that setUp() is never called explicitly
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
        "integrationUser1,password1,USER",
        "integrationUser2,password2,USER",
        "adminUser,adminPass,ADMIN"
    })
    void testRegisterAndLoginParameterized(String username, String password, String expectedRole) throws Exception {
        // Prepare DTO
        UserRequestDTO registerDto = new UserRequestDTO();
        registerDto.setUsername(username);
        registerDto.setPassword(password);

        // ---------------- Register ----------------
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));

        // Verify DB persistence
        User savedUser = userRepository.findByUsername(username).orElseThrow();
        assertEquals(username, savedUser.getUsername());

        // ---------------- Login ----------------
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(expectedRole))
                .andExpect(jsonPath("$.token").exists());
    }
}
