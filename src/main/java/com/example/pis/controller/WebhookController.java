package com.example.pis.controller;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pis.exception.BadPayloadException;
import com.example.pis.exception.CryptographyException;
import com.example.pis.exception.InvalidSignatureException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ObjectMapper mapper; 
    private final EventIdStore eventIdStore; 

    private final String stripeWebhookSecret;
    private final String airtelWebhookSecret;
    private final String mtnWebhookSecret;
    private final Duration allowedTimestampSkew;

    public WebhookController(
            ObjectMapper mapper,
            @Value("${stripe.webhookSecret}") String stripeWebhookSecret,
            @Value("${airtel.webhookSecret}") String airtelWebhookSecret,
            @Value("${mtn.webhookSecret}") String mtnWebhookSecret,
            @Value("${webhook.allowedTimestampSeconds:300}") long allowedTimestampSeconds) {

        this.mapper = mapper;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.airtelWebhookSecret = airtelWebhookSecret;
        this.mtnWebhookSecret = mtnWebhookSecret;
        this.allowedTimestampSkew = Duration.ofSeconds(allowedTimestampSeconds);

        // Inline default implementation of EventIdStore
        this.eventIdStore = new EventIdStore() {
            private final Set<String> processed = ConcurrentHashMap.newKeySet();

            @Override
            public boolean isProcessed(String eventId) {
                return processed.contains(eventId);
            }

            @Override
            public void markProcessed(String eventId) {
                processed.add(eventId);
            }
        };
    }

    // ---------------- Stripe Webhook ----------------
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader) {

        if (payload == null || sigHeader == null) {
            throw new BadPayloadException("Missing payload or signature");
        }

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            log.info("Stripe event received: type={}, id={}", event.getType(), event.getId());

            if (eventIdStore.isProcessed(event.getId())) {
                return ResponseEntity.ok("Already processed");
            }

            eventIdStore.markProcessed(event.getId());
            return ResponseEntity.ok("Received");

        } catch (SignatureVerificationException e) {
            throw new InvalidSignatureException("Invalid Stripe signature", e);
        } catch (IllegalArgumentException e) {
            throw new BadPayloadException("Invalid Stripe payload", e);
        }
    }

    // ---------------- MTN Webhook ----------------
    @PostMapping("/mtn")
    public ResponseEntity<String> handleMtnWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "X-MTN-Signature", required = false) String signature,
            @RequestHeader(name = "X-Request-Timestamp", required = false) String timestampHeader) {

        if (payload == null || signature == null) {
            throw new BadPayloadException("Missing payload or signature");
        }

        if (!isTimestampFresh(timestampHeader)) {
            throw new BadPayloadException("Invalid timestamp");
        }

        try {
            Optional<byte[]> expectedBytesOpt = decodeSignatureToBytes(signature);
            if (expectedBytesOpt.isEmpty()) {
                throw new InvalidSignatureException("Invalid MTN signature format");
            }

            try {
                if (!verifyHmacRawBytes(payload.getBytes(StandardCharsets.UTF_8),
                        mtnWebhookSecret.getBytes(StandardCharsets.UTF_8), expectedBytesOpt.get())) {
                    throw new InvalidSignatureException("Invalid MTN signature");
                }
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new CryptographyException("MTN cryptography error", e);
            }

            Optional<String> externalId = extractEventId(payload);
            if (externalId.isPresent() && eventIdStore.isProcessed(externalId.get())) {
                return ResponseEntity.ok("Already processed");
            }

            log.info("MTN webhook received");
            externalId.ifPresent(eventIdStore::markProcessed);
            return ResponseEntity.ok("Received");

        } catch (IllegalArgumentException e) {
            throw new BadPayloadException("Invalid MTN payload", e);
        }
    }

    // ---------------- Airtel Webhook ----------------
    @PostMapping("/airtel")
    public ResponseEntity<String> handleAirtelWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "X-Airtel-Signature", required = false) String signature,
            @RequestHeader(name = "X-Request-Timestamp", required = false) String timestampHeader) {

        if (payload == null || signature == null) {
            throw new BadPayloadException("Missing payload or signature");
        }

        if (!isTimestampFresh(timestampHeader)) {
            throw new BadPayloadException("Invalid timestamp");
        }

        try {
            Optional<byte[]> expectedBytesOpt = decodeSignatureToBytes(signature);
            if (expectedBytesOpt.isEmpty()) {
                throw new InvalidSignatureException("Invalid Airtel signature format");
            }

            try {
                if (!verifyHmacRawBytes(payload.getBytes(StandardCharsets.UTF_8),
                        airtelWebhookSecret.getBytes(StandardCharsets.UTF_8), expectedBytesOpt.get())) {
                    throw new InvalidSignatureException("Invalid Airtel signature");
                }
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new CryptographyException("Airtel cryptography error", e);
            }

            Optional<String> externalId = extractEventId(payload);
            if (externalId.isPresent() && eventIdStore.isProcessed(externalId.get())) {
                return ResponseEntity.ok("Already processed");
            }

            log.info("Airtel webhook received");
            externalId.ifPresent(eventIdStore::markProcessed);
            return ResponseEntity.ok("Received");

        } catch (IllegalArgumentException e) {
            throw new BadPayloadException("Invalid Airtel payload", e);
        }
    }

    // ---------- Utility Methods ----------

    private Optional<byte[]> decodeSignatureToBytes(String signature) {
        if (signature == null) return Optional.empty();
        String s = signature;
        int idx = s.indexOf('=');
        if (idx > 0) s = s.substring(idx + 1);

        try {
            return Optional.of(Base64.getDecoder().decode(s));
        } catch (IllegalArgumentException ignore) {}

        if (s.length() % 2 == 0) {
            try {
                byte[] out = new byte[s.length() / 2];
                for (int i = 0; i < s.length(); i += 2) {
                    out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
                }
                return Optional.of(out);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private boolean verifyHmacRawBytes(byte[] dataBytes, byte[] secretBytes, byte[] expected)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        byte[] actual = mac.doFinal(dataBytes);
        return MessageDigest.isEqual(actual, expected);
    }

    private boolean isTimestampFresh(String timestampHeader) {
        if (timestampHeader == null) return true;
        try {
            Instant ts = Instant.ofEpochSecond(Long.parseLong(timestampHeader));
            Duration delta = Duration.between(ts, Instant.now()).abs();
            return delta.compareTo(allowedTimestampSkew) <= 0;
        } catch (NumberFormatException nfe) {
            try {
                Instant ts = Instant.parse(timestampHeader);
                Duration delta = Duration.between(ts, Instant.now()).abs();
                return delta.compareTo(allowedTimestampSkew) <= 0;
            } catch (Exception e) {
                log.warn("Unable to parse timestamp header: {}", timestampHeader);
                return false;
            }
        }
    }

    private Optional<String> extractEventId(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            if (root.has("id")) return Optional.of(root.get("id").asText());
            if (root.has("eventId")) return Optional.of(root.get("eventId").asText());
            if (root.has("reference")) return Optional.of(root.get("reference").asText());
            if (root.has("transactionId")) return Optional.of(root.get("transactionId").asText());
        } catch (JsonProcessingException e) {
            log.debug("Failed to extract event id: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public interface EventIdStore {
        boolean isProcessed(String eventId);
        void markProcessed(String eventId);
    }
}
