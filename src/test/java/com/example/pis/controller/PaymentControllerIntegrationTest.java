package com.example.pis.controller;

import com.example.pis.dto.MomoCollectRequest;
import com.example.pis.entity.PaymentTransaction;
import com.example.pis.repository.PaymentTransactionRepository;
import com.example.pis.service.AirtelService;
import com.example.pis.service.MtnService;
import com.example.pis.service.StripeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the PaymentController endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository txRepo;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private MtnService mtnService;

    @Autowired
    private AirtelService airtelService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        txRepo.deleteAll();
    }

    @Test
    void createStripeIntent_shouldSaveTransaction() throws Exception {
        MomoCollectRequest req = new MomoCollectRequest("stripe", "256776111222", 1000L, "refIntStripe");

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
        MomoCollectRequest req = new MomoCollectRequest("mtn", "256776222333", 2000L, "refIntMtn");

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
        MomoCollectRequest req = new MomoCollectRequest("airtel", "256700333444", 3000L, "refIntAir");

        mockMvc.perform(post("/api/payments/airtel/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertEquals(1L, txRepo.count());
        PaymentTransaction tx = txRepo.findAll().get(0);
        assertEquals("airtel", tx.getProvider());
    }
}
