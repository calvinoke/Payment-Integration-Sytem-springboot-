package com.example.pis.controller;

import com.example.pis.dto.AirtelResponseDTO;
import com.example.pis.dto.MomoCollectRequest;
import com.example.pis.dto.MtnResponseDTO;
import com.example.pis.repository.PaymentTransactionRepository;
import com.example.pis.service.AirtelService;
import com.example.pis.service.MtnService;
import com.example.pis.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Full unit tests for PaymentController (Stripe, MTN, Airtel).
 */
class PaymentControllerTest {

    private StripeService stripeService;
    private MtnService mtnService;
    private AirtelService airtelService;
    private PaymentTransactionRepository txRepo;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        stripeService = mock(StripeService.class);
        mtnService = mock(MtnService.class);
        airtelService = mock(AirtelService.class);
        txRepo = mock(PaymentTransactionRepository.class);

        controller = new PaymentController(stripeService, mtnService, airtelService, txRepo);
        controller.setConfiguredApiKey("test-api-key"); // package-private setter
    }

    // ---------------- Stripe ----------------
    @Test
    void createStripeIntent_shouldReturnClientSecretAndReference() throws Exception {
        MomoCollectRequest req = new MomoCollectRequest("stripe", "256700123456", 1000L, "ref123");
        when(stripeService.createPaymentIntent(req.amount(), "usd")).thenReturn("secret123");

        ResponseEntity<?> response = controller.createStripeIntent("test-api-key", req);
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertNotNull(body);
        assertEquals("secret123", body.get("clientSecret"));
        assertEquals("ref123", body.get("reference"));
        verify(txRepo, times(1)).save(any());
    }

    // ---------------- MTN Collection ----------------
    @Test
    void mtnCollect_shouldReturnMtnResponse() {
        MomoCollectRequest req = new MomoCollectRequest("mtn", "256700123456", 500L, "refMtn");
        MtnResponseDTO dto = new MtnResponseDTO("SUCCESS", "Payment initiated");

        when(mtnService.initiateCollection(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.mtnCollect("test-api-key", req);
        MtnResponseDTO body = (MtnResponseDTO) response.getBody();

        assertEquals("SUCCESS", body.getStatus());
        assertEquals("Payment initiated", body.getBody());
        verify(txRepo, times(1)).save(any());
        verify(mtnService, times(1))
                .initiateCollection(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ---------------- MTN Withdrawal ----------------
    @Test
    void mtnWithdraw_shouldReturnMtnResponse() {
        MomoCollectRequest req = new MomoCollectRequest("mtn", "256700123456", 500L, "refMtnWithdraw");
        MtnResponseDTO dto = new MtnResponseDTO("SUCCESS", "Withdrawal initiated");

        when(mtnService.initiateWithdrawal(anyString(), anyLong(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.mtnWithdraw("test-api-key", req);
        MtnResponseDTO body = (MtnResponseDTO) response.getBody();

        assertEquals("SUCCESS", body.getStatus());
        assertEquals("Withdrawal initiated", body.getBody());
        verify(txRepo, times(1)).save(any());
        verify(mtnService, times(1))
                .initiateWithdrawal(anyString(), anyLong(), anyString());
    }

    // ---------------- Airtel Collection ----------------
    @Test
    void airtelCollect_shouldReturnAirtelResponse() {
        MomoCollectRequest req = new MomoCollectRequest("airtel", "256700987654", 800L, "refAir");
        AirtelResponseDTO dto = new AirtelResponseDTO("SUCCESS", "Payment initiated");

        when(airtelService.initiateCollection(anyString(), anyLong(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.airtelCollect("test-api-key", req);
        AirtelResponseDTO body = (AirtelResponseDTO) response.getBody();

        assertEquals("SUCCESS", body.getStatus());
        assertEquals("Payment initiated", body.getBody());
        verify(txRepo, times(1)).save(any());
        verify(airtelService, times(1))
                .initiateCollection(anyString(), anyLong(), anyString());
    }

    // ---------------- Airtel Withdrawal ----------------
    @Test
    void airtelWithdraw_shouldReturnAirtelResponse() {
        MomoCollectRequest req = new MomoCollectRequest("airtel", "256700987654", 800L, "refAirWithdraw");
        AirtelResponseDTO dto = new AirtelResponseDTO("SUCCESS", "Withdrawal initiated");

        when(airtelService.initiateWithdrawal(anyString(), anyLong(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.airtelWithdraw("test-api-key", req);
        AirtelResponseDTO body = (AirtelResponseDTO) response.getBody();

        assertEquals("SUCCESS", body.getStatus());
        assertEquals("Withdrawal initiated", body.getBody());
        verify(txRepo, times(1)).save(any());
        verify(airtelService, times(1))
                .initiateWithdrawal(anyString(), anyLong(), anyString());
    }
}
