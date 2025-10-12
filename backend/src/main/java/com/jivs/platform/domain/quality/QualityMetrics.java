package com.jivs.platform.domain.quality;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Embeddable class representing data quality metrics
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class QualityMetrics {

    private int totalChecks;

    private int passedChecks;

    private int failedChecks;

    private double passRate;

    private int criticalIssues;

    private int majorIssues;

    private int minorIssues;

    @ElementCollection
    private Map<RuleType, Long> checksByType = new HashMap<>();

    // Getters and Setters
    public int getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(int totalChecks) {
        this.totalChecks = totalChecks;
    }

    public int getPassedChecks() {
        return passedChecks;
    }

    public void setPassedChecks(int passedChecks) {
        this.passedChecks = passedChecks;
    }

    public int getFailedChecks() {
        return failedChecks;
    }

    public void setFailedChecks(int failedChecks) {
        this.failedChecks = failedChecks;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public int getCriticalIssues() {
        return criticalIssues;
    }

    public void setCriticalIssues(int criticalIssues) {
        this.criticalIssues = criticalIssues;
    }

    public int getMajorIssues() {
        return majorIssues;
    }

    public void setMajorIssues(int majorIssues) {
        this.majorIssues = majorIssues;
    }

    public int getMinorIssues() {
        return minorIssues;
    }

    public void setMinorIssues(int minorIssues) {
        this.minorIssues = minorIssues;
    }

    public Map<RuleType, Long> getChecksByType() {
        return checksByType;
    }

    public void setChecksByType(Map<RuleType, Long> checksByType) {
        this.checksByType = checksByType;
    }
}
