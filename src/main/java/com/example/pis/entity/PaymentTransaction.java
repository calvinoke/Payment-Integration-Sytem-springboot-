package com.example.pis.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
            @Index(name = "idx_reference", columnList = "reference", unique = true),
            @Index(name = "idx_provider_txid", columnList = "provider, provider_transaction_id")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Optional fields to store provider responses
    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(name = "mtn_response", columnDefinition = "TEXT")
    private String mtnResponse;

    @Column(name = "airtel_response", columnDefinition = "TEXT")
    private String airtelResponse;

    /* ---------- Constructors ---------- */
    public PaymentTransaction() {}

    public PaymentTransaction(String provider, String providerTransactionId,
                              String reference, Long amount, String currency, String status) {
        this.provider = provider;
        this.providerTransactionId = providerTransactionId;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    /* ---------- Getters & Setters ---------- */
    public Long getId() { return id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getMtnResponse() { return mtnResponse; }
    public void setMtnResponse(String mtnResponse) { this.mtnResponse = mtnResponse; }

    public String getAirtelResponse() { return airtelResponse; }
    public void setAirtelResponse(String airtelResponse) { this.airtelResponse = airtelResponse; }

    /* ---------- Payment Update Methods ---------- */

    /**
     * Updates the transaction with provider response and status.
     *
     * @param provider          The payment provider ("mtn", "airtel", "stripe")
     * @param responseBody      The raw response from the provider
     * @param providerTxId      Provider-specific transaction ID (if any)
     * @param success           True if the transaction succeeded, false otherwise
     */
    public void updatePaymentResponse(String provider, String responseBody, String providerTxId, boolean success) {
        if ("mtn".equalsIgnoreCase(provider)) {
            this.mtnResponse = responseBody;
        } else if ("airtel".equalsIgnoreCase(provider)) {
            this.airtelResponse = responseBody;
        } else if ("stripe".equalsIgnoreCase(provider)) {
            this.clientSecret = providerTxId; // Use clientSecret as identifier for Stripe
        }

        this.providerTransactionId = providerTxId;
        this.status = success ? "SUCCESS" : "FAILED";
    }

    /** Marks transaction as initiated */
    public void markInitiated() {
        this.status = "INITIATED";
    }

    /** Marks transaction as failed */
    public void markFailed() {
        this.status = "FAILED";
    }

    /* ---------- Utility ---------- */
    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "id=" + id +
                ", provider='" + provider + '\'' +
                ", providerTransactionId='" + providerTransactionId + '\'' +
                ", reference='" + reference + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", clientSecret='" + clientSecret + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentTransaction that)) return false;
        return Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() { return Objects.hash(reference); }
}
