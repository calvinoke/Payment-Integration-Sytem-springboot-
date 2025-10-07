package com.example.pis.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

public class StripeTransferResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String transferId;

    @NotNull
    private String reference;

    public StripeTransferResponseDTO() {}

    public StripeTransferResponseDTO(@NotNull String transferId, @NotNull String reference) {
        this.transferId = transferId;
        this.reference = reference;
    }

    @NotNull
    public String getTransferId() { return transferId; }
    public void setTransferId(@NotNull String transferId) { this.transferId = transferId; }

    @NotNull
    public String getReference() { return reference; }
    public void setReference(@NotNull String reference) { this.reference = reference; }

    @Override
    public String toString() {
        return "StripeTransferResponseDTO{" +
                "transferId='" + transferId + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
