package com.jivs.platform.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.domain.Webhook;
import com.jivs.platform.domain.WebhookEvent;
import com.jivs.platform.repository.WebhookEventRepository;
import com.jivs.platform.repository.WebhookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service for managing and triggering webhooks
 */
@Service
@Slf4j
@Transactional
public class WebhookService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Trigger webhooks for a specific event
     */
    @Async("webhookExecutor")
    public void triggerWebhooks(String eventType, Map<String, Object> eventData) {
        log.info("Triggering webhooks for event: {}", eventType);

        List<Webhook> webhooks = webhookRepository.findByEventTypesContainingAndActive(eventType, true);

        for (Webhook webhook : webhooks) {
            try {
                sendWebhook(webhook, eventType, eventData);
            } catch (Exception e) {
                log.error("Failed to send webhook {}: {}", webhook.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Send webhook with retry logic
     */
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2),
        retryFor = {RestClientException.class}
    )
    public void sendWebhook(Webhook webhook, String eventType, Map<String, Object> eventData) {
        long startTime = System.currentTimeMillis();

        WebhookEvent event = new WebhookEvent();
        event.setWebhook(webhook);
        event.setEventType(eventType);
        event.setStatus("PENDING");

        try {
            // Prepare payload
            String payload = objectMapper.writeValueAsString(eventData);
            event.setPayload(payload);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add HMAC signature if secret is configured
            if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
                String signature = generateHmacSignature(payload, webhook.getSecret());
                headers.set("X-Webhook-Signature", signature);
            }

            // Add event type header
            headers.set("X-Event-Type", eventType);

            // Add timestamp
            headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));

            // Create request
            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            // Send request
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            // Update event with success
            event.setStatus("SUCCESS");
            event.setResponseCode(response.getStatusCode().value());
            event.setResponseBody(response.getBody());
            event.setCompletedAt(new Date());

            // Update webhook stats
            webhook.setSuccessCount(webhook.getSuccessCount() + 1);
            webhook.setLastTriggeredAt(new Date());

        } catch (RestClientException e) {
            log.error("Webhook {} failed: {}", webhook.getId(), e.getMessage());

            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            event.setCompletedAt(new Date());

            // Update webhook stats
            webhook.setFailureCount(webhook.getFailureCount() + 1);

            // Deactivate webhook if failure rate is too high
            if (shouldDeactivateWebhook(webhook)) {
                webhook.setActive(false);
                log.warn("Webhook {} deactivated due to high failure rate", webhook.getId());
            }

            throw e; // Re-throw for retry mechanism

        } catch (Exception e) {
            log.error("Unexpected error sending webhook {}: {}", webhook.getId(), e.getMessage(), e);

            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            event.setCompletedAt(new Date());

            webhook.setFailureCount(webhook.getFailureCount() + 1);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            event.setDurationMs(duration);

            webhookEventRepository.save(event);
            webhookRepository.save(webhook);
        }
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHmacSignature(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC signature: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Determine if webhook should be deactivated based on failure rate
     */
    private boolean shouldDeactivateWebhook(Webhook webhook) {
        long totalCalls = webhook.getSuccessCount() + webhook.getFailureCount();
        if (totalCalls < 10) {
            return false; // Not enough data
        }

        double failureRate = (double) webhook.getFailureCount() / totalCalls;
        return failureRate > 0.8; // Deactivate if >80% failure rate
    }

    /**
     * Test webhook
     */
    public WebhookEvent testWebhook(Long webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        Map<String, Object> testData = Map.of(
            "test", true,
            "message", "This is a test webhook",
            "timestamp", System.currentTimeMillis()
        );

        sendWebhook(webhook, "TEST", testData);

        // Return the most recent event
        return webhookEventRepository.findTopByWebhookOrderByCreatedAtDesc(webhook);
    }

    /**
     * Get webhook statistics
     */
    public Map<String, Object> getWebhookStats(Long webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        long totalCalls = webhook.getSuccessCount() + webhook.getFailureCount();
        double successRate = totalCalls > 0
            ? (double) webhook.getSuccessCount() / totalCalls * 100
            : 0.0;

        List<WebhookEvent> recentEvents = webhookEventRepository
            .findTop10ByWebhookOrderByCreatedAtDesc(webhook);

        return Map.of(
            "webhookId", webhook.getId(),
            "totalCalls", totalCalls,
            "successCount", webhook.getSuccessCount(),
            "failureCount", webhook.getFailureCount(),
            "successRate", successRate,
            "lastTriggered", webhook.getLastTriggeredAt(),
            "active", webhook.isActive(),
            "recentEvents", recentEvents
        );
    }
}
