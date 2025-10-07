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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private StripeService stripeService;
    private MtnService mtnService;
    private AirtelService airtelService;
    private PaymentTransactionRepository txRepo;
    private PaymentController controller;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        stripeService = mock(StripeService.class);
        mtnService = mock(MtnService.class);
        airtelService = mock(AirtelService.class);
        txRepo = mock(PaymentTransactionRepository.class);

        controller = new PaymentController(stripeService, mtnService, airtelService, txRepo);
    }

    /* ---------------- Stripe ---------------- */

    @SuppressWarnings("unchecked")
    @Test
    void createStripeIntent_shouldReturnClientSecretAndReference() {
        MomoCollectRequest req = new MomoCollectRequest("stripe", "256700123456", 1000L, "USD", "ref123");
        when(stripeService.createPaymentIntent(req.amount(), req.currency(), req.reference()))
                .thenReturn("secret123");

        ResponseEntity<?> response = controller.createStripeIntent("dummy-api-key", req);
        assertNotNull(response, "Response should not be null");

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody(), "Response body should not be null");

        assertEquals("secret123", Objects.requireNonNull(body.get("clientSecret"), "clientSecret should not be null"));
        assertEquals("ref123", Objects.requireNonNull(body.get("reference"), "reference should not be null"));
        verify(txRepo, times(1)).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void stripeTransfer_shouldReturnTransferIdAndReference() {
        Map<String, Object> body = Map.of(
                "amount", 500L,
                "currency", "USD",
                "connectedAccountId", "acct_123"
        );

        when(stripeService.sendTransfer(anyLong(), anyString(), anyString()))
                .thenReturn("transfer_123");

        ResponseEntity<?> response = controller.stripeTransfer("dummy-api-key", body);
        Map<String, Object> respBody = Objects.requireNonNull((Map<String, Object>) response.getBody(), "Response body should not be null");

        assertEquals("transfer_123", Objects.requireNonNull(respBody.get("transferId"), "transferId should not be null"));
        assertNotNull(respBody.get("reference"), "reference should not be null");
        verify(txRepo, times(1)).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void stripePayout_shouldReturnPayoutIdAndReference() {
        Map<String, Object> body = Map.of("amount", 700L, "currency", "USD");

        when(stripeService.createPayout(anyLong(), anyString()))
                .thenReturn("payout_456");

        ResponseEntity<?> response = controller.stripePayout("dummy-api-key", body);
        Map<String, Object> respBody = Objects.requireNonNull((Map<String, Object>) response.getBody(), "Response body should not be null");

        assertEquals("payout_456", Objects.requireNonNull(respBody.get("payoutId"), "payoutId should not be null"));
        assertNotNull(respBody.get("reference"), "reference should not be null");
        verify(txRepo, times(1)).save(any());
    }

    /* ---------------- MTN Collection ---------------- */

    @Test
    void mtnCollect_shouldReturnMtnResponse() {
        MomoCollectRequest req = new MomoCollectRequest("mtn", "256700123456", 500L, "UGX", "refMtn");
        MtnResponseDTO dto = new MtnResponseDTO("SUCCESS", "Payment initiated");

        when(mtnService.initiateCollection(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.mtnCollect("dummy-api-key", req);
        MtnResponseDTO body = Objects.requireNonNull((MtnResponseDTO) response.getBody(), "Response body should not be null");

        assertEquals("SUCCESS", Objects.requireNonNull(body.getStatus(), "Status should not be null"));
        assertEquals("Payment initiated", Objects.requireNonNull(body.getBody(), "Body text should not be null"));

        verify(txRepo, times(1)).save(any());
        verify(mtnService, times(1))
                .initiateCollection(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void mtnWithdraw_shouldReturnMtnResponse() {
        MomoCollectRequest req = new MomoCollectRequest("mtn", "256700123456", 500L, "UGX", "refMtnWithdraw");
        MtnResponseDTO dto = new MtnResponseDTO("SUCCESS", "Withdrawal initiated");

        when(mtnService.initiateWithdrawal(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.mtnWithdraw("dummy-api-key", req);
        MtnResponseDTO body = Objects.requireNonNull((MtnResponseDTO) response.getBody(), "Response body should not be null");

        assertEquals("SUCCESS", Objects.requireNonNull(body.getStatus(), "Status should not be null"));
        assertEquals("Withdrawal initiated", Objects.requireNonNull(body.getBody(), "Body text should not be null"));

        verify(txRepo, times(1)).save(any());
        verify(mtnService, times(1))
                .initiateWithdrawal(anyString(), anyLong(), anyString(), anyString());
    }

    /* ---------------- Airtel Collection ---------------- */

    @Test
    void airtelCollect_shouldReturnAirtelResponse() {
        MomoCollectRequest req = new MomoCollectRequest("airtel", "256700987654", 800L, "UGX", "refAir");
        AirtelResponseDTO dto = new AirtelResponseDTO("SUCCESS", "Payment initiated");

        when(airtelService.initiateCollection(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.airtelCollect("dummy-api-key", req);
        AirtelResponseDTO body = Objects.requireNonNull((AirtelResponseDTO) response.getBody(), "Response body should not be null");

        assertEquals("SUCCESS", Objects.requireNonNull(body.getStatus(), "Status should not be null"));
        assertEquals("Payment initiated", Objects.requireNonNull(body.getBody(), "Body text should not be null"));

        verify(txRepo, times(1)).save(any());
        verify(airtelService, times(1))
                .initiateCollection(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void airtelWithdraw_shouldReturnAirtelResponse() {
        MomoCollectRequest req = new MomoCollectRequest("airtel", "256700987654", 800L, "UGX", "refAirWithdraw");
        AirtelResponseDTO dto = new AirtelResponseDTO("SUCCESS", "Withdrawal initiated");

        when(airtelService.initiateWithdrawal(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.airtelWithdraw("dummy-api-key", req);
        AirtelResponseDTO body = Objects.requireNonNull((AirtelResponseDTO) response.getBody(), "Response body should not be null");

        assertEquals("SUCCESS", Objects.requireNonNull(body.getStatus(), "Status should not be null"));
        assertEquals("Withdrawal initiated", Objects.requireNonNull(body.getBody(), "Body text should not be null"));

        verify(txRepo, times(1)).save(any());
        verify(airtelService, times(1))
                .initiateWithdrawal(anyString(), anyLong(), anyString(), anyString());
    }
}
