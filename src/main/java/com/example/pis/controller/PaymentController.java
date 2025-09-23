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

    /* ---------- Test-only setter ---------- */
    void setConfiguredApiKey(String apiKey) {
        this.configuredApiKey = apiKey;
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

    /* ---------- Stripe ---------- */
    @PostMapping("/stripe/create-payment-intent")
    @Transactional
    public ResponseEntity<?> createStripeIntent(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (StringUtils.hasText(req.reference())) {
            Optional<PaymentTransaction> existing = txRepo.findByReference(req.reference());
            if (existing.isPresent()) {
                log.info("Stripe intent exists: {}", req.reference());
                return ResponseEntity.ok(Map.of(
                        "clientSecret", existing.get().getClientSecret(),
                        "reference", existing.get().getReference(),
                        "status", existing.get().getStatus()
                ));
            }
        }

        try {
            Long amount = req.amount();
            String currency = "usd";
            String clientSecret = stripeService.createPaymentIntent(amount, currency);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setProvider("stripe");
            tx.setAmount(amount);
            tx.setCurrency(currency);
            tx.setStatus("CREATED");
            tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : "stripe-" + UUID.randomUUID());
            tx.setCreatedAt(Instant.now());
            tx.setClientSecret(clientSecret);
            txRepo.save(tx);

            log.info("Stripe intent created: ref={}, amount={}", tx.getReference(), amount);
            return ResponseEntity.ok(Map.of("clientSecret", clientSecret, "reference", tx.getReference()));

        } catch (Exception e) {
            log.error("Stripe intent error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment intent"));
        }
    }

    /* ---------- MTN Collection ---------- */
    @PostMapping("/mtn/collect")
    @Transactional
    public ResponseEntity<?> mtnCollect(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (StringUtils.hasText(req.reference())) {
            Optional<PaymentTransaction> existing = txRepo.findByReference(req.reference());
            if (existing.isPresent()) {
                log.info("MTN collect exists: {}", req.reference());
                return ResponseEntity.ok(existing.get());
            }
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setProvider("mtn");
        tx.setAmount(req.amount());
        tx.setCurrency("UGX");
        tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : UUID.randomUUID().toString());
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        txRepo.save(tx);

        try {
            MtnResponseDTO response = mtnService.initiateCollection(
                    String.valueOf(req.amount()),
                    req.phone(),
                    tx.getReference(),
                    "Payment request",
                    "Payment to merchant"
            );

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getBody())) {
                tx.setStatus("INITIATED");
            } else {
                tx.setStatus("FAILED");
            }
            txRepo.save(tx);

            log.info("MTN collect initiated: ref={}, phone={}", tx.getReference(), redactPhone(req.phone()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            tx.setStatus("FAILED");
            txRepo.save(tx);
            log.error("MTN collect error: ref={}, {}", tx.getReference(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate MTN collection"));
        }
    }

    /* ---------- MTN Withdrawal ---------- */
    @PostMapping("/mtn/withdraw")
    @Transactional
    public ResponseEntity<?> mtnWithdraw(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (StringUtils.hasText(req.reference())) {
            Optional<PaymentTransaction> existing = txRepo.findByReference(req.reference());
            if (existing.isPresent()) {
                log.info("MTN withdraw exists: {}", req.reference());
                return ResponseEntity.ok(existing.get());
            }
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setProvider("mtn");
        tx.setAmount(req.amount());
        tx.setCurrency("UGX");
        tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : UUID.randomUUID().toString());
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        txRepo.save(tx);

        try {
            MtnResponseDTO response = mtnService.initiateWithdrawal(
                    req.phone(),
                    req.amount(),
                    tx.getReference()
            );

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getBody())) {
                tx.setStatus("INITIATED");
            } else {
                tx.setStatus("FAILED");
            }
            txRepo.save(tx);

            log.info("MTN withdraw initiated: ref={}, phone={}", tx.getReference(), redactPhone(req.phone()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            tx.setStatus("FAILED");
            txRepo.save(tx);
            log.error("MTN withdraw error: ref={}, {}", tx.getReference(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate MTN withdrawal"));
        }
    }

    /* ---------- Airtel Collection ---------- */
    @PostMapping("/airtel/collect")
    @Transactional
    public ResponseEntity<?> airtelCollect(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (StringUtils.hasText(req.reference())) {
            Optional<PaymentTransaction> existing = txRepo.findByReference(req.reference());
            if (existing.isPresent()) {
                log.info("Airtel collect exists: {}", req.reference());
                return ResponseEntity.ok(existing.get());
            }
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setProvider("airtel");
        tx.setAmount(req.amount());
        tx.setCurrency("UGX");
        tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : UUID.randomUUID().toString());
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        txRepo.save(tx);

        try {
            AirtelResponseDTO response = airtelService.initiateCollection(
                    req.phone(),
                    req.amount(),
                    tx.getReference()
            );

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getBody())) {
                tx.setStatus("INITIATED");
            } else {
                tx.setStatus("FAILED");
            }
            txRepo.save(tx);

            log.info("Airtel collect initiated: ref={}, phone={}", tx.getReference(), redactPhone(req.phone()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            tx.setStatus("FAILED");
            txRepo.save(tx);
            log.error("Airtel collect error: ref={}, {}", tx.getReference(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate Airtel collection"));
        }
    }

    /* ---------- Airtel Withdrawal ---------- */
    @PostMapping("/airtel/withdraw")
    @Transactional
    public ResponseEntity<?> airtelWithdraw(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MomoCollectRequest req
    ) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (StringUtils.hasText(req.reference())) {
            Optional<PaymentTransaction> existing = txRepo.findByReference(req.reference());
            if (existing.isPresent()) {
                log.info("Airtel withdraw exists: {}", req.reference());
                return ResponseEntity.ok(existing.get());
            }
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setProvider("airtel");
        tx.setAmount(req.amount());
        tx.setCurrency("UGX");
        tx.setReference(StringUtils.hasText(req.reference()) ? req.reference() : UUID.randomUUID().toString());
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        txRepo.save(tx);

        try {
            AirtelResponseDTO response = airtelService.initiateWithdrawal(
                    req.phone(),
                    req.amount(),
                    tx.getReference()
            );

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getBody())) {
                tx.setStatus("INITIATED");
            } else {
                tx.setStatus("FAILED");
            }
            txRepo.save(tx);

            log.info("Airtel withdraw initiated: ref={}, phone={}", tx.getReference(), redactPhone(req.phone()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            tx.setStatus("FAILED");
            txRepo.save(tx);
            log.error("Airtel withdraw error: ref={}, {}", tx.getReference(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate Airtel withdrawal"));
        }
    }
}
