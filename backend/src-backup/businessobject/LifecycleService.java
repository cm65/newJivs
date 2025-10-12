package com.jivs.platform.service.businessobject;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.domain.businessobject.BusinessObjectLifecycle;
import com.jivs.platform.domain.businessobject.LifecycleEvent;
import com.jivs.platform.repository.BusinessObjectLifecycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing business object lifecycle events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LifecycleService {

    private final BusinessObjectLifecycleRepository lifecycleRepository;

    /**
     * Initialize lifecycle for new business object
     */
    @Transactional
    public void initializeLifecycle(BusinessObject businessObject) {
        recordEvent(businessObject, LifecycleEvent.CREATED, "Business object created");
    }

    /**
     * Record update event
     */
    @Transactional
    public void recordUpdate(BusinessObject businessObject) {
        recordEvent(businessObject, LifecycleEvent.UPDATED, "Business object updated");
    }

    /**
     * Record archival event
     */
    @Transactional
    public void recordArchival(BusinessObject businessObject) {
        recordEvent(businessObject, LifecycleEvent.ARCHIVED, "Business object archived");
    }

    /**
     * Record deletion event
     */
    @Transactional
    public void recordDeletion(BusinessObject businessObject) {
        recordEvent(businessObject, LifecycleEvent.DELETED, "Business object deleted");
    }

    /**
     * Record custom event
     */
    @Transactional
    public void recordEvent(BusinessObject businessObject, LifecycleEvent event, String description) {
        BusinessObjectLifecycle lifecycle = new BusinessObjectLifecycle();
        lifecycle.setBusinessObject(businessObject);
        lifecycle.setEvent(event);
        lifecycle.setDescription(description);
        lifecycle.setEventDate(LocalDateTime.now());
        lifecycle.setUserId(getCurrentUserId());

        lifecycleRepository.save(lifecycle);
        log.debug("Recorded lifecycle event {} for business object {}", event, businessObject.getId());
    }

    /**
     * Get lifecycle history for business object
     */
    public List<BusinessObjectLifecycle> getLifecycleHistory(BusinessObject businessObject) {
        return lifecycleRepository.findByBusinessObjectOrderByEventDateDesc(businessObject);
    }

    /**
     * Get lifecycle history for business object with event filter
     */
    public List<BusinessObjectLifecycle> getLifecycleHistory(
            BusinessObject businessObject,
            LifecycleEvent event) {
        return lifecycleRepository.findByBusinessObjectAndEvent(businessObject, event);
    }

    private Long getCurrentUserId() {
        // Get current user from security context
        return 1L; // Simplified
    }
}
