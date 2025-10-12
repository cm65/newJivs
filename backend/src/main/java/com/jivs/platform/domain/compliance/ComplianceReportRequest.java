package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a request to generate a compliance report
 * This is not an entity, just a data transfer object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportRequest {

    private Regulation regulation;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private String reportTitle;

    private List<String> includeMetrics = new ArrayList<>();

    private Boolean includeDetailedStatistics = true;

    private Boolean includeRecommendations = true;

    private Boolean includeDataBreaches = true;

    private Boolean includeConsentAnalysis = true;

    private String format = "PDF";  // PDF, CSV, EXCEL, JSON

    private String recipientEmail;

    private Boolean autoPublish = false;

    /**
     * Helper method to set default values for last month
     */
    public void setDefaultLastMonth() {
        LocalDateTime now = LocalDateTime.now();
        this.periodEnd = now.withDayOfMonth(1).minusDays(1);
        this.periodStart = periodEnd.withDayOfMonth(1);
    }

    /**
     * Helper method to set default values for last quarter
     */
    public void setDefaultLastQuarter() {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;

        this.periodEnd = now.withMonth(quarterStartMonth).withDayOfMonth(1).minusDays(1);
        this.periodStart = periodEnd.minusMonths(2).withDayOfMonth(1);
    }

    /**
     * Helper method to set default values for last year
     */
    public void setDefaultLastYear() {
        LocalDateTime now = LocalDateTime.now();
        this.periodEnd = now.withMonth(1).withDayOfMonth(1).minusDays(1);
        this.periodStart = periodEnd.withDayOfMonth(1);
    }

    /**
     * Convenience methods for ComplianceService compatibility
     */
    public LocalDateTime getStartDate() {
        return periodStart;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.periodStart = startDate;
    }

    public LocalDateTime getEndDate() {
        return periodEnd;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.periodEnd = endDate;
    }
}
