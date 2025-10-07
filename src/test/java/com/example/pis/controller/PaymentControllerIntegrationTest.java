package com.example.pis.controller;

import com.example.pis.dto.MomoCollectRequest;
import com.example.pis.dto.MtnResponseDTO;
import com.example.pis.dto.AirtelResponseDTO;
import com.example.pis.entity.PaymentTransaction;
import com.example.pis.repository.PaymentTransactionRepository;
import com.example.pis.service.AirtelService;
import com.example.pis.service.MtnService;
import com.example.pis.service.StripeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(PaymentControllerIntegrationTest.MockServiceConfig.class) // <- Important
class PaymentControllerIntegrationTest {

    // This TestConfiguration will be imported and used by Spring
    @TestConfiguration
    static class MockServiceConfig {
        @Bean
        public StripeService stripeService() {
            StripeService mock = mock(StripeService.class);
            when(mock.createPaymentIntent(anyLong(), anyString(), anyString()))
                    .thenReturn("secret_test");
            return mock;
        }

        @Bean
        public MtnService mtnService() {
            MtnService mock = mock(MtnService.class);
            when(mock.initiateCollection(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new MtnResponseDTO("200", "success"));
            return mock;
        }

        @Bean
        public AirtelService airtelService() {
            AirtelService mock = mock(AirtelService.class);
            when(mock.initiateCollection(anyString(), anyLong(), anyString(), anyString()))
                    .thenReturn(new AirtelResponseDTO("200", "success"));
            return mock;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository txRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        txRepo.deleteAll();
    }

    @Test
    void createStripeIntent_shouldSaveTransaction() throws Exception {
        MomoCollectRequest req = new MomoCollectRequest(
                "stripe", "256776111222", 1000L, "USD", "refIntStripe"
        );

        mockMvc.perform(post("/api/payments/stripe/create-payment-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertEquals(1L, txRepo.count());
        PaymentTransaction tx = txRepo.findAll().get(0);
        assertEquals("stripe", tx.getProvider());
    }

    @Test
    void mtnCollect_shouldSaveTransaction() throws Exception {
        MomoCollectRequest req = new MomoCollectRequest(
                "mtn", "256776222333", 2000L, "UGX", "refIntMtn"
        );

        mockMvc.perform(post("/api/payments/mtn/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertEquals(1L, txRepo.count());
        PaymentTransaction tx = txRepo.findAll().get(0);
        assertEquals("mtn", tx.getProvider());
    }

    @Test
    void airtelCollect_shouldSaveTransaction() throws Exception {
        MomoCollectRequest req = new MomoCollectRequest(
                "airtel", "256700333444", 3000L, "UGX", "refIntAir"
        );

        mockMvc.perform(post("/api/payments/airtel/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertEquals(1L, txRepo.count());
        PaymentTransaction tx = txRepo.findAll().get(0);
        assertEquals("airtel", tx.getProvider());
    }
}
