package com.example.pis.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

public class MtnResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String status; // "SUCCESS", "FAILED"

    @NotNull
    private String body;

    public MtnResponseDTO() {
        this.status = "";
        this.body = "";
    }

    public MtnResponseDTO(@NotNull String status, @NotNull String body) {
        this.status = status;
        this.body = body;
    }

    @NotNull
    public String getStatus() { return status; }

    public void setStatus(@NotNull String status) { this.status = status; }

    @NotNull
    public String getBody() { return body; }

    public void setBody(@NotNull String body) { this.body = body; }

    @Override
    public String toString() {
        return "MtnResponseDTO{" +
                "status='" + status + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
