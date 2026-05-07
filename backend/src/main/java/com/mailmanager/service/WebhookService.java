package com.mailmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailmanager.config.WebhookProperties;
import com.mailmanager.domain.DeliveryStatus;
import com.mailmanager.domain.MailMessage;
import com.mailmanager.domain.WebhookDelivery;
import com.mailmanager.domain.WebhookEndpoint;
import com.mailmanager.repository.WebhookDeliveryRepository;
import com.mailmanager.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;
    
    // Shared HTTP client for better resource management
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void dispatchMailReceived(MailMessage mailMessage) {
        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findByActiveTrue().stream()
                .filter(endpoint -> endpoint.getEventType().equalsIgnoreCase("mail.received"))
                .filter(endpoint -> endpoint.getSubjectKeyword() == null
                        || endpoint.getSubjectKeyword().isBlank()
                        || (mailMessage.getSubject() != null && mailMessage.getSubject().contains(endpoint.getSubjectKeyword())))
                .toList();
        for (WebhookEndpoint endpoint : endpoints) {
            deliver(endpoint, mailMessage);
        }
    }

    public String sign(String payload, String timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign webhook payload", exception);
        }
    }

    private void deliver(WebhookEndpoint endpoint, MailMessage mailMessage) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setWebhookEndpoint(endpoint);
        delivery.setMailMessage(mailMessage);
        webhookDeliveryRepository.save(delivery);

        String payload;
        try {
            LinkedHashMap<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("event", "mail.received");
            payloadMap.put("mail_id", mailMessage.getId());
            payloadMap.put("account_email", mailMessage.getAccount().getEmail());
            payloadMap.put("subject", mailMessage.getSubject());
            payloadMap.put("parsed_json", mailMessage.getParsedJson() == null ? "{}" : mailMessage.getParsedJson());
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize webhook payload", exception);
        }

        for (int attempt = 1; attempt <= webhookProperties.retryCount(); attempt++) {
            String timestamp = Instant.now().toString();
            String signature = sign(payload, timestamp, endpoint.getSharedSecret());
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint.getUrl()))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Signature", signature)
                        .timeout(Duration.ofSeconds(webhookProperties.timeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                delivery.setAttemptCount(attempt);
                delivery.setResponseCode(response.statusCode());
                delivery.setResponseBody(truncate(response.body()));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    delivery.setStatus(DeliveryStatus.SUCCESS);
                    webhookDeliveryRepository.save(delivery);
                    return;
                }
                delivery.setLastErrorMessage("Non-2xx response");
            } catch (Exception exception) {
                delivery.setAttemptCount(attempt);
                delivery.setLastErrorMessage(truncate(exception.getMessage()));
            }
        }
        delivery.setStatus(DeliveryStatus.FAILED);
        webhookDeliveryRepository.save(delivery);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
