package com.example.pis.dto;

import java.io.Serializable;

public class StripePayoutRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long amount;
    private String currency;
    private String reference;

    public StripePayoutRequestDTO() {}

    public StripePayoutRequestDTO(Long amount, String currency, String reference) {
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
    }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    // Optional: convert to entity for saving
    // public PaymentTransaction toTransactionEntity() { ... }
}
