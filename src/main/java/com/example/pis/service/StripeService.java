package com.example.pis.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.pis.enums.SupportedCurrency;
import com.example.pis.exception.PaymentProcessingException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;

import jakarta.annotation.PostConstruct;

/**
 * Service for managing Stripe payments:
 * - Receiving payments from customers (PaymentIntent)
 * - Sending payouts to your bank or connected accounts
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
     * Creates a PaymentIntent (receive payment from a customer).
     *
     * @param amount      Amount in the smallest currency unit (e.g., cents)
     * @param currency    ISO-4217 currency code, e.g., "USD"
     * @param referenceId Unique reference for idempotency
     * @return client secret to confirm on frontend
     */
    public String createPaymentIntent(Long amount, String currency, String referenceId) {
        validateRequest(amount, currency);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency.toUpperCase());
        params.put("payment_method_types", List.of("card"));

        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(referenceId != null ? referenceId : UUID.randomUUID().toString())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);

            logger.info("Stripe PaymentIntent created. ID={}, Amount={}, Currency={}, Reference={}",
                    paymentIntent.getId(), amount, currency.toUpperCase(), referenceId);

            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe PaymentIntent. Amount={}, Currency={}, Reference={}, Error={}",
                    amount, currency.toUpperCase(), referenceId, e.getMessage(), e);
            throw new PaymentProcessingException("Stripe PaymentIntent creation failed", e);
        }
    }

    /**
     * Sends money from your Stripe balance to a connected account (Stripe Connect).
     *
     * @param amount        Amount in the smallest currency unit
     * @param currency      Currency code (e.g., "USD")
     * @param connectedAcct The connected Stripe Account ID (acct_xxx)
     */
    public String sendTransfer(Long amount, String currency, String connectedAcct) {
        validateRequest(amount, currency);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency.toUpperCase());
        params.put("destination", connectedAcct);

        try {
            Transfer transfer = Transfer.create(params);
            logger.info("Stripe Transfer successful. ID={}, Amount={}, Currency={}, Destination={}",
                    transfer.getId(), amount, currency.toUpperCase(), connectedAcct);
            return transfer.getId();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe Transfer. Amount={}, Currency={}, Destination={}, Error={}",
                    amount, currency.toUpperCase(), connectedAcct, e.getMessage(), e);
            throw new PaymentProcessingException("Stripe transfer failed", e);
        }
    }

    /**
     * Payout funds to your bank account (requires Stripe balance).
     *
     * @param amount   Amount in the smallest currency unit
     * @param currency Currency code
     */
    public String createPayout(Long amount, String currency) {
        validateRequest(amount, currency);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency.toUpperCase());

        try {
            Payout payout = Payout.create(params);
            logger.info("Stripe Payout created. ID={}, Amount={}, Currency={}",
                    payout.getId(), amount, currency.toUpperCase());
            return payout.getId();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe Payout. Amount={}, Currency={}, Error={}",
                    amount, currency.toUpperCase(), e.getMessage(), e);
            throw new PaymentProcessingException("Stripe payout failed", e);
        }
    }

    /**
     * Validates request params and ensures the currency is supported.
     */
    private void validateRequest(Long amount, String currency) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive (smallest currency unit).");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency code is required.");
        }

        currency = currency.toUpperCase();
        if (!SupportedCurrency.CODES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }
}
