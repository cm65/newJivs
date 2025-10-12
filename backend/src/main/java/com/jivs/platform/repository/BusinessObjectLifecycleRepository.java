package com.jivs.platform.repository;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.domain.businessobject.BusinessObjectLifecycle;
import com.jivs.platform.domain.businessobject.LifecycleEventType;
import com.jivs.platform.domain.businessobject.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessObjectLifecycle entity
 */
@Repository
public interface BusinessObjectLifecycleRepository extends JpaRepository<BusinessObjectLifecycle, Long> {

    /**
     * Find lifecycle by business object
     */
    Optional<BusinessObjectLifecycle> findByBusinessObject(BusinessObject businessObject);

    /**
     * Find lifecycles by current state
     */
    List<BusinessObjectLifecycle> findByCurrentState(LifecycleState state);

    /**
     * Find latest lifecycle for business object
     */
    Optional<BusinessObjectLifecycle> findFirstByBusinessObjectOrderByUpdatedAtDesc(BusinessObject businessObject);

    /**
     * Find lifecycle history for business object ordered by event date descending
     */
    List<BusinessObjectLifecycle> findByBusinessObjectOrderByEventDateDesc(BusinessObject businessObject);

    /**
     * Find lifecycle history for business object filtered by event type
     */
    List<BusinessObjectLifecycle> findByBusinessObjectAndEvent(BusinessObject businessObject, LifecycleEventType event);
}
