package com.example.pis.controller;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

/**
 * Handles webhook callbacks from payment providers: Stripe, MTN, Airtel.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${stripe.webhookSecret}")
    private String stripeWebhookSecret;

    @Value("${airtel.webhookSecret}")
    private String airtelWebhookSecret;

    @Value("${mtn.webhookSecret}")
    private String mtnWebhookSecret;

    // ---------------- Stripe Webhook ----------------
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            log.info("Stripe event received: type={}, id={}", event.getType(), event.getId());
            // TODO: process Stripe event
            return ResponseEntity.ok("Received");
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    // ---------------- MTN Webhook ----------------
    @PostMapping("/mtn")
    public ResponseEntity<String> handleMtnWebhook(
            @RequestBody String payload,
            @RequestHeader("X-MTN-Signature") String signature // adjust header name if MTN uses different one
    ) {
        try {
            if (!verifyHmacSha256(payload, mtnWebhookSecret, signature)) {
                log.warn("Invalid MTN signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
            log.info("MTN webhook received: {}", payload);
            // TODO: parse and process MTN event with mapper.readTree(payload) or POJO
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            log.error("MTN webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    // ---------------- Airtel Webhook ----------------
    @PostMapping("/airtel")
    public ResponseEntity<String> handleAirtelWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Airtel-Signature") String signature // Airtel header name per docs
    ) {
        try {
            if (!verifyHmacSha256(payload, airtelWebhookSecret, signature)) {
                log.warn("Invalid Airtel signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
            log.info("Airtel webhook received: {}", payload);
            // TODO: parse payload: e.g.
            // AirtelEvent event = mapper.readValue(payload, AirtelEvent.class);
            // process event
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            log.error("Airtel webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    // ---------- Utility ----------
    private boolean verifyHmacSha256(String data, String secret, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes());
            String actual = Base64.getEncoder().encodeToString(rawHmac);
            return actual.equals(expectedSignature);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage(), e);
            return false;
        }
    }
}
