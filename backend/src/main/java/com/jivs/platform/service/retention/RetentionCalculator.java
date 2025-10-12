package com.jivs.platform.service.retention;

import com.jivs.platform.domain.retention.RetentionPolicy;
import com.jivs.platform.domain.retention.RetentionUnit;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calculator for retention dates and periods
 */
@Component
public class RetentionCalculator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetentionCalculator.class);

    /**
     * Calculate cutoff date based on retention period
     */
    public LocalDateTime calculateCutoffDate(int retentionPeriod, RetentionUnit unit) {
        LocalDateTime now = LocalDateTime.now();

        switch (unit) {
            case DAYS:
                return now.minusDays(retentionPeriod);
            case WEEKS:
                return now.minusWeeks(retentionPeriod);
            case MONTHS:
                return now.minusMonths(retentionPeriod);
            case YEARS:
                return now.minusYears(retentionPeriod);
            default:
                throw new IllegalArgumentException("Unknown retention unit: " + unit);
        }
    }

    /**
     * Calculate expiry date for a record
     */
    public LocalDateTime calculateExpiryDate(RetentionPolicy policy, LocalDateTime recordCreationDate) {
        switch (policy.getRetentionUnit()) {
            case DAYS:
                return recordCreationDate.plusDays(policy.getRetentionPeriod());
            case WEEKS:
                return recordCreationDate.plusWeeks(policy.getRetentionPeriod());
            case MONTHS:
                return recordCreationDate.plusMonths(policy.getRetentionPeriod());
            case YEARS:
                return recordCreationDate.plusYears(policy.getRetentionPeriod());
            default:
                throw new IllegalArgumentException("Unknown retention unit: " + policy.getRetentionUnit());
        }
    }

    /**
     * Calculate days until expiry
     */
    public long calculateDaysUntilExpiry(LocalDateTime expiryDate) {
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }

    /**
     * Calculate retention period in days
     */
    public long calculatePeriodInDays(int period, RetentionUnit unit) {
        switch (unit) {
            case DAYS:
                return period;
            case WEEKS:
                return period * 7L;
            case MONTHS:
                return period * 30L; // Approximation
            case YEARS:
                return period * 365L; // Approximation
            default:
                return 0;
        }
    }
}