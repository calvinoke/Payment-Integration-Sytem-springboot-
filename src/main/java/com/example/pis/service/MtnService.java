package com.example.pis.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.pis.dto.MtnResponseDTO;

import jakarta.annotation.PostConstruct;

/**
 * Service for interacting with MTN Mobile Money API for collections and withdrawals.
 */
@Service
public class MtnService {

    private static final Logger logger = LoggerFactory.getLogger(MtnService.class);

    @Value("${mtn.clientId}")
    private String clientId;

    @Value("${mtn.clientSecret}")
    private String clientSecret;

    @Value("${mtn.apiUrl}")
    private String apiUrl;

    @Value("${mtn.subscriptionKey}")
    private String subscriptionKey;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        if (clientId == null || clientSecret == null || apiUrl == null || subscriptionKey == null) {
            throw new IllegalStateException("Incomplete MTN configuration");
        }

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restTemplate = new RestTemplate(factory);

        logger.info("MtnService initialized successfully");
    }

    /** Fetches an access token from MTN API using client credentials */
    public String getAccessToken() {
        String tokenUrl = apiUrl + "/token/";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId.trim(), clientSecret.trim());
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to get MTN token");
        }

        Map<String, Object> body = Optional.ofNullable(response.getBody())
                .orElseThrow(() -> new IllegalStateException("MTN token response missing body"));

        Object accessToken = body.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("MTN token response missing access_token");
        }

        return accessToken.toString();
    }

    /** Initiates a collection (payment request) on MTN Mobile Money */
    public MtnResponseDTO initiateCollection(String amount, String msisdn, String externalId, String payerMessage, String payeeNote, String currency) {
        validateRequest(amount, msisdn, externalId);

        if (currency == null || currency.isBlank()) currency = "UGX";
        currency = currency.toUpperCase();

        String token = getAccessToken();
        String url = apiUrl + "/requesttopay";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Reference-Id", UUID.randomUUID().toString());
        headers.set("X-Target-Environment", "sandbox");
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amount,
                "currency", currency,
                "externalId", externalId,
                "payer", Map.of("partyIdType", "MSISDN", "partyId", msisdn),
                "payerMessage", payerMessage,
                "payeeNote", payeeNote
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

        int status = resp.getStatusCode().value();
        String respBody = Optional.ofNullable(resp.getBody()).orElse("");

        logger.info("MTN collection initiated: {} [{}]", externalId, currency);
        return new MtnResponseDTO(String.valueOf(status), respBody);
    }

    /** Initiates a withdrawal (payout) on MTN Mobile Money */
    public MtnResponseDTO initiateWithdrawal(String msisdn, Long amount, String reference, String currency) {
        validateRequest(amount.toString(), msisdn, reference);

        if (currency == null || currency.isBlank()) currency = "UGX";
        currency = currency.toUpperCase();

        String token = getAccessToken();
        String url = apiUrl + "/disbursement";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Reference-Id", UUID.randomUUID().toString());
        headers.set("X-Target-Environment", "sandbox");
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amount.toString(),
                "currency", currency,
                "externalId", reference,
                "payee", Map.of("partyIdType", "MSISDN", "partyId", msisdn)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

        int status = resp.getStatusCode().value();
        String respBody = Optional.ofNullable(resp.getBody()).orElse("");

        logger.info("MTN withdrawal initiated: {} [{}]", reference, currency);
        return new MtnResponseDTO(String.valueOf(status), respBody);
    }

    /** Basic request validation */
    private void validateRequest(String amount, String msisdn, String externalId) {
        if (amount == null || amount.isBlank()) throw new IllegalArgumentException("Amount is required");
        if (msisdn == null || msisdn.isBlank()) throw new IllegalArgumentException("MSISDN is required");
        if (externalId == null || externalId.isBlank()) throw new IllegalArgumentException("External ID is required");
    }
}
