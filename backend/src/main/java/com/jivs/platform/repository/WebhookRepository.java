package com.jivs.platform.repository;

import com.jivs.platform.domain.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    /**
     * Find active webhooks for a specific event type
     */
    List<Webhook> findByEventTypesContainingAndActive(String eventType, boolean active);

    /**
     * Find all active webhooks
     */
    List<Webhook> findByActive(boolean active);

    /**
     * Find webhooks by name
     */
    List<Webhook> findByNameContainingIgnoreCase(String name);
}
