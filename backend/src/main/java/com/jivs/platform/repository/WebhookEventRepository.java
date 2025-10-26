package com.jivs.platform.repository;

import com.jivs.platform.domain.Webhook;
import com.jivs.platform.domain.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    /**
     * Find events by webhook
     */
    Page<WebhookEvent> findByWebhook(Webhook webhook, Pageable pageable);

    /**
     * Find events by webhook and status
     */
    List<WebhookEvent> findByWebhookAndStatus(Webhook webhook, String status);

    /**
     * Find recent events for a webhook
     */
    List<WebhookEvent> findTop10ByWebhookOrderByCreatedAtDesc(Webhook webhook);

    /**
     * Find most recent event for a webhook
     */
    WebhookEvent findTopByWebhookOrderByCreatedAtDesc(Webhook webhook);

    /**
     * Find failed events for retry
     */
    @Query("SELECT we FROM WebhookEvent we WHERE we.status = 'FAILED' " +
           "AND we.attemptCount < :maxAttempts " +
           "AND we.createdAt > :since")
    List<WebhookEvent> findFailedEventsForRetry(
        @Param("maxAttempts") int maxAttempts,
        @Param("since") Date since
    );

    /**
     * Count events by status
     */
    Long countByWebhookAndStatus(Webhook webhook, String status);

    /**
     * Get webhook statistics
     */
    @Query("SELECT we.status as status, COUNT(we) as count, AVG(we.durationMs) as avgDuration " +
           "FROM WebhookEvent we WHERE we.webhook = :webhook " +
           "GROUP BY we.status")
    List<Object[]> getWebhookStatistics(@Param("webhook") Webhook webhook);
}
