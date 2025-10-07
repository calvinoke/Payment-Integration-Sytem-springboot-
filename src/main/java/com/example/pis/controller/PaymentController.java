package com.example.pis.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pis.dto.AirtelResponseDTO;
import com.example.pis.dto.MomoCollectRequest;
import com.example.pis.dto.MtnResponseDTO;
import com.example.pis.entity.PaymentTransaction;
import com.example.pis.enums.SupportedCurrency;
import com.example.pis.exception.PaymentProcessingException;
import com.example.pis.repository.PaymentTransactionRepository;
import com.example.pis.service.AirtelService;
import com.example.pis.service.MtnService;
import com.example.pis.service.StripeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final StripeService stripeService;
    private final MtnService mtnService;
    private final AirtelService airtelService;
    private final PaymentTransactionRepository txRepo;

    @Value("${app.api.key}")
    private String configuredApiKey;

    public PaymentController(
            StripeService stripeService,
            MtnService mtnService,
            AirtelService airtelService,
            PaymentTransactionRepository txRepo
    ) {
        this.stripeService = stripeService;
        this.mtnService = mtnService;
        this.airtelService = airtelService;
        this.txRepo = txRepo;
    }

    /* ---------- Helpers ---------- */
    private boolean isAuthorized(String apiKey) {
        return StringUtils.hasText(apiKey) && apiKey.equals(configuredApiKey);
    }

    private String redactPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        int keep = 3;
        int len = phone.length();
        return phone.substring(0, keep) + "..." + phone.substring(len - 1);
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "USD";
        String code = currency.toUpperCase();
        if (!SupportedCurrency.CODES.contains(code)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        return code;
    }

    private Optional<PaymentTransaction> findExisting(String reference) {
        return StringUtils.hasText(reference) ? txRepo.findByReference(reference) : Optional.empty();
    }

    private PaymentTransaction startTransaction(String provider, MomoCollectRequest req, String currency) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setProvider(provider);
        tx.setAmount(req.amount());
        tx.setCurrency(currency);
        tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : UUID.randomUUID().toString());
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        txRepo.save(tx);
        return tx;
    }

    private void failTransaction(PaymentTransaction tx, String message, RuntimeException ex) {
        tx.markFailed();
        txRepo.save(tx);
        log.error("{}: ref={}, {}", message, tx.getReference(), ex.getMessage(), ex);
    }

    /* ============================================================== 
       STRIPE ENDPOINTS (multi-currency) 
       ============================================================== */

    @PostMapping("/stripe/create-payment-intent")
    @Transactional
    public ResponseEntity<?> createStripeIntent(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Optional<PaymentTransaction> existing = findExisting(req.reference());
        if (existing.isPresent()) {
            log.info("Stripe intent exists: {}", req.reference());
            return ResponseEntity.ok(Map.of(
                    "clientSecret", existing.get().getClientSecret(),
                    "reference", existing.get().getReference(),
                    "status", existing.get().getStatus()
            ));
        }

        String currency = validateCurrency(req.currency());
        try {
            String clientSecret = stripeService.createPaymentIntent(req.amount(), currency, req.reference());
            PaymentTransaction tx = startTransaction("stripe", req, currency);
            tx.setClientSecret(clientSecret);
            tx.markInitiated();
            txRepo.save(tx);

            log.info("Stripe intent created: ref={}, amount={}, currency={}", tx.getReference(), req.amount(), currency);
            return ResponseEntity.ok(Map.of("clientSecret", clientSecret, "reference", tx.getReference()));

        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to create Stripe payment intent", ex);
        }
    }

    @PostMapping("/stripe/transfer")
    @Transactional
    public ResponseEntity<?> stripeTransfer(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> body
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Long amount = ((Number) body.get("amount")).longValue();
        String currency = validateCurrency((String) body.getOrDefault("currency", "USD"));
        String connectedAcct = (String) body.get("connectedAccountId");

        try {
            String transferId = stripeService.sendTransfer(amount, currency, connectedAcct);
            PaymentTransaction tx = new PaymentTransaction();
            tx.setProvider("stripe");
            tx.setAmount(amount);
            tx.setCurrency(currency);
            tx.setReference("transfer-" + UUID.randomUUID());
            tx.updatePaymentResponse("stripe", "TRANSFER_SUCCESS", transferId, true);
            txRepo.save(tx);

            return ResponseEntity.ok(Map.of("transferId", transferId, "reference", tx.getReference()));
        } catch (Exception ex) {
            throw new PaymentProcessingException("Stripe transfer failed", ex);
        }
    }

    @PostMapping("/stripe/payout")
    @Transactional
    public ResponseEntity<?> stripePayout(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> body
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Long amount = ((Number) body.get("amount")).longValue();
        String currency = validateCurrency((String) body.getOrDefault("currency", "USD"));

        try {
            String payoutId = stripeService.createPayout(amount, currency);
            PaymentTransaction tx = new PaymentTransaction();
            tx.setProvider("stripe");
            tx.setAmount(amount);
            tx.setCurrency(currency);
            tx.setReference("payout-" + UUID.randomUUID());
            tx.updatePaymentResponse("stripe", "PAYOUT_SUCCESS", payoutId, true);
            txRepo.save(tx);

            return ResponseEntity.ok(Map.of("payoutId", payoutId, "reference", tx.getReference()));
        } catch (Exception ex) {
            throw new PaymentProcessingException("Stripe payout failed", ex);
        }
    }

    /* ============================================================== 
       MTN & AIRTEL ENDPOINTS 
       ============================================================== */

    @PostMapping("/mtn/collect")
    @Transactional
    public ResponseEntity<?> mtnCollect(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Optional<PaymentTransaction> existing = findExisting(req.reference());
        if (existing.isPresent()) return ResponseEntity.ok(existing.get());

        String currency = validateCurrency(req.currency());
        PaymentTransaction tx = startTransaction("mtn", req, currency);
        try {
            MtnResponseDTO response = mtnService.initiateCollection(
                    String.valueOf(req.amount()),
                    req.phone(),
                    tx.getReference(),
                    "Payment request",
                    "Payment to merchant",
                    currency
            );
            boolean success = response != null && "SUCCESS".equalsIgnoreCase(response.getBody());
            tx.updatePaymentResponse("mtn", response != null ? response.toString() : null, null, success);
            txRepo.save(tx);

            log.info("MTN collect initiated: ref={}, phone={}, currency={}", tx.getReference(), redactPhone(req.phone()), currency);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            failTransaction(tx, "Failed to initiate MTN collection", ex);
            throw new PaymentProcessingException("Failed to initiate MTN collection", ex);
        }
    }

    @PostMapping("/mtn/withdraw")
    @Transactional
    public ResponseEntity<?> mtnWithdraw(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Optional<PaymentTransaction> existing = findExisting(req.reference());
        if (existing.isPresent()) return ResponseEntity.ok(existing.get());

        String currency = validateCurrency(req.currency());
        PaymentTransaction tx = startTransaction("mtn", req, currency);
        try {
            MtnResponseDTO response = mtnService.initiateWithdrawal(req.phone(), req.amount(), tx.getReference(), currency);
            boolean success = response != null && "SUCCESS".equalsIgnoreCase(response.getBody());
            tx.updatePaymentResponse("mtn", response != null ? response.toString() : null, null, success);
            txRepo.save(tx);

            log.info("MTN withdraw initiated: ref={}, phone={}, currency={}", tx.getReference(), redactPhone(req.phone()), currency);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            failTransaction(tx, "Failed to initiate MTN withdrawal", ex);
            throw new PaymentProcessingException("Failed to initiate MTN withdrawal", ex);
        }
    }

    @PostMapping("/airtel/collect")
    @Transactional
    public ResponseEntity<?> airtelCollect(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Optional<PaymentTransaction> existing = findExisting(req.reference());
        if (existing.isPresent()) return ResponseEntity.ok(existing.get());

        String currency = validateCurrency(req.currency());
        PaymentTransaction tx = startTransaction("airtel", req, currency);
        try {
            AirtelResponseDTO response = airtelService.initiateCollection(req.phone(), req.amount(), tx.getReference(), currency);
            boolean success = response != null && "SUCCESS".equalsIgnoreCase(response.getBody());
            tx.updatePaymentResponse("airtel", response != null ? response.toString() : null, null, success);
            txRepo.save(tx);

            log.info("Airtel collect initiated: ref={}, phone={}, currency={}", tx.getReference(), redactPhone(req.phone()), currency);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            failTransaction(tx, "Failed to initiate Airtel collection", ex);
            throw new PaymentProcessingException("Failed to initiate Airtel collection", ex);
        }
    }

    @PostMapping("/airtel/withdraw")
    @Transactional
    public ResponseEntity<?> airtelWithdraw(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        Optional<PaymentTransaction> existing = findExisting(req.reference());
        if (existing.isPresent()) return ResponseEntity.ok(existing.get());

        String currency = validateCurrency(req.currency());
        PaymentTransaction tx = startTransaction("airtel", req, currency);
        try {
            AirtelResponseDTO response = airtelService.initiateWithdrawal(req.phone(), req.amount(), tx.getReference(), currency);
            boolean success = response != null && "SUCCESS".equalsIgnoreCase(response.getBody());
            tx.updatePaymentResponse("airtel", response != null ? response.toString() : null, null, success);
            txRepo.save(tx);

            log.info("Airtel withdraw initiated: ref={}, phone={}, currency={}", tx.getReference(), redactPhone(req.phone()), currency);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            failTransaction(tx, "Failed to initiate Airtel withdrawal", ex);
            throw new PaymentProcessingException("Failed to initiate Airtel withdrawal", ex);
        }
    }
}
