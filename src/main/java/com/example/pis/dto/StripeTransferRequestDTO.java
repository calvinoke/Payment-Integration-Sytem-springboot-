package com.example.pis.dto;

import java.io.Serializable;

public class StripeTransferRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long amount;
    private String currency;
    private String connectedAccountId;
    private String reference;

    public StripeTransferRequestDTO() {}

    public StripeTransferRequestDTO(Long amount, String currency, String connectedAccountId, String reference) {
        this.amount = amount;
        this.currency = currency;
        this.connectedAccountId = connectedAccountId;
        this.reference = reference;
    }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getConnectedAccountId() { return connectedAccountId; }
    public void setConnectedAccountId(String connectedAccountId) { this.connectedAccountId = connectedAccountId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    // Optional: convert to entity for saving
    // public PaymentTransaction toTransactionEntity() { ... }
}
