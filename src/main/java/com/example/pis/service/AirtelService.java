package com.example.pis.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.pis.dto.AirtelResponseDTO;

import jakarta.annotation.PostConstruct;

/**
 * Service for interacting with Airtel Money API for payments and disbursements.
 */
@Service
public class AirtelService {

    private static final Logger logger = LoggerFactory.getLogger(AirtelService.class);

    @Value("${airtel.apiUrl}")
    private String apiUrl;

    @Value("${airtel.clientId}")
    private String clientId;

    @Value("${airtel.clientSecret}")
    private String clientSecret;

    @Value("${airtel.apiKey}")
    private String apiKey;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        if (apiUrl == null || clientId == null || clientSecret == null || apiKey == null) {
            throw new IllegalStateException("Incomplete Airtel configuration");
        }

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restTemplate = new RestTemplate(factory);

        logger.info("AirtelService initialized");
    }

    /** Obtains OAuth token from Airtel API */
    private String getToken() {
        String tokenUrl = apiUrl + "/v1/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId.trim(), clientSecret.trim());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to obtain Airtel token, status: " + response.getStatusCode());
        }

        Map<String, Object> body = Optional.ofNullable(response.getBody())
                .orElseThrow(() -> new IllegalStateException("Airtel token response missing body"));

        Object token = body.get("access_token");
        if (token == null) throw new IllegalStateException("Airtel token missing access_token");

        return token.toString();
    }

    /** Initiates a collection (payment) request */
    public AirtelResponseDTO initiateCollection(String phone, Long amount, String reference) {
        validateRequest(phone, amount, reference);

        String token = getToken();
        String url = apiUrl + "/collection/v1_0/requesttopay";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Callback-Url", "https://your-domain.com/webhooks/airtel");
        headers.set("X-Reference-Id", UUID.randomUUID().toString());
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amount.toString(),
                "currency", "UGX",
                "externalId", reference,
                "payer", Map.of("partyIdType", "MSISDN", "partyId", phone)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

        int status = resp.getStatusCode().value();
        String respBody = Optional.ofNullable(resp.getBody()).orElse("");

        logger.info("Airtel collection initiated: {}", reference);
        return new AirtelResponseDTO(String.valueOf(status), respBody);
    }

    /** Initiates a withdrawal (disbursement) request */
    public AirtelResponseDTO initiateWithdrawal(String phone, Long amount, String reference) {
        validateRequest(phone, amount, reference);

        String token = getToken();
        String url = apiUrl + "/disbursement/v1_0/transfer";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Callback-Url", "https://your-domain.com/webhooks/airtel");
        headers.set("X-Reference-Id", UUID.randomUUID().toString());
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amount.toString(),
                "currency", "UGX",
                "externalId", reference,
                "payee", Map.of("partyIdType", "MSISDN", "partyId", phone)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

        int status = resp.getStatusCode().value();
        String respBody = Optional.ofNullable(resp.getBody()).orElse("");

        logger.info("Airtel withdrawal initiated: {}", reference);
        return new AirtelResponseDTO(String.valueOf(status), respBody);
    }

    /** Validates required parameters */
    private void validateRequest(String phone, Long amount, String reference) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Phone is required");
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("Reference is required");
    }
}
