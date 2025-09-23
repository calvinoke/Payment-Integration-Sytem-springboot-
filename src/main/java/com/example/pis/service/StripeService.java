package com.example.pis.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating and managing Stripe payment intents.
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void init() {
        if (stripeSecret == null || stripeSecret.isBlank()) {
            logger.error("Stripe secret key is not configured! Application cannot process payments.");
            throw new IllegalStateException("Stripe secret key not configured.");
        }
        Stripe.apiKey = stripeSecret.trim();
        logger.info("Stripe API key initialized successfully.");
    }

    /**
     * Creates a PaymentIntent with Stripe and returns its client secret.
     *
     * @param amount   Amount in the smallest currency unit (e.g., cents)
     * @param currency ISO-4217 currency code, e.g., "usd" or "eur"
     * @return client secret of the created PaymentIntent
     * @throws IllegalArgumentException if parameters are invalid
     * @throws StripeException          if the Stripe API call fails
     */
    public String createPaymentIntent(Long amount, String currency) throws StripeException {
        validateRequest(amount, currency);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency.toLowerCase());
        params.put("payment_method_types", List.of("card"));

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            logger.info("Stripe PaymentIntent created. ID={}, Amount={}, Currency={}",
                    paymentIntent.getId(), amount, currency);
            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe PaymentIntent: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateRequest(Long amount, String currency) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number of smallest currency units.");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency code must be provided.");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO-4217 code.");
        }
    }
}
