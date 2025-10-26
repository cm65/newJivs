package com.jivs.platform.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Central event publisher for cross-module communication
 * Publishes events to both Kafka (for async processing) and WebSocket (for real-time UI updates)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformEventPublisher {

    private final KafkaTemplate<String, PlatformEvent> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String KAFKA_TOPIC = "jivs-platform-events";

    /**
     * Publish event to Kafka for async processing
     * @param event The event to publish
     */
    public void publish(PlatformEvent event) {
        try {
            log.debug("Publishing event: {} for entity {}:{}", 
                    event.getEventType(), event.getEntityType(), event.getEntityId());
            
            CompletableFuture<SendResult<String, PlatformEvent>> future = 
                    kafkaTemplate.send(KAFKA_TOPIC, event.getEntityId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published successfully: {}", event.getEventId());
                } else {
                    log.error("Failed to publish event: {}", event.getEventId(), ex);
                }
            });
            
            // Also broadcast to WebSocket for real-time UI updates
            broadcastToWebSocket(event);
            
        } catch (Exception e) {
            log.error("Error publishing event: {}", event.getEventType(), e);
        }
    }

    /**
     * Broadcast event to WebSocket subscribers
     * @param event The event to broadcast
     */
    private void broadcastToWebSocket(PlatformEvent event) {
        try {
            String destination = "/topic/" + event.getSource().name().toLowerCase() + 
                                 "/" + event.getEntityId();
            
            messagingTemplate.convertAndSend(destination, event);
            
            log.debug("Event broadcasted to WebSocket: {}", destination);
        } catch (Exception e) {
            log.warn("Failed to broadcast event to WebSocket: {}", event.getEventId(), e);
        }
    }

    /**
     * Publish event synchronously (waits for confirmation)
     * Use for critical events where confirmation is required
     * @param event The event to publish
     * @return true if published successfully
     */
    public boolean publishSync(PlatformEvent event) {
        try {
            SendResult<String, PlatformEvent> result = 
                    kafkaTemplate.send(KAFKA_TOPIC, event.getEntityId().toString(), event).get();
            
            log.debug("Event published synchronously: {}", event.getEventId());
            broadcastToWebSocket(event);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to publish event synchronously: {}", event.getEventType(), e);
            return false;
        }
    }
}
