package com.jivs.platform.service.notification;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for generating notification content from templates
 */
@Service
public class NotificationTemplateService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationTemplateService.class);

    /**
     * Generate email HTML content from template
     */
    public String generateEmailContent(NotificationType type, Map<String, Object> data) {
        log.debug("Generating email content for type: {}", type);

        switch (type) {
            case EXTRACTION_COMPLETE:
                return generateExtractionCompleteEmail(data);
            case EXTRACTION_FAILED:
                return generateExtractionFailedEmail(data);
            case MIGRATION_COMPLETE:
                return generateMigrationCompleteEmail(data);
            case MIGRATION_FAILED:
                return generateMigrationFailedEmail(data);
            case DATA_QUALITY_ALERT:
                return generateDataQualityAlertEmail(data);
            case RETENTION_ACTION_DUE:
                return generateRetentionActionEmail(data);
            case COMPLIANCE_REQUEST:
                return generateComplianceRequestEmail(data);
            case SYSTEM_ALERT:
                return generateSystemAlertEmail(data);
            case APPROVAL_REQUIRED:
                return generateApprovalRequiredEmail(data);
            default:
                return generateGenericEmail(data);
        }
    }

    /**
     * Generate SMS content from template
     */
    public String generateSmsContent(NotificationType type, Map<String, Object> data) {
        log.debug("Generating SMS content for type: {}", type);

        switch (type) {
            case EXTRACTION_COMPLETE:
                return String.format("Extraction %s completed successfully", data.get("extractionId"));
            case EXTRACTION_FAILED:
                return String.format("Extraction %s failed. Check dashboard for details", data.get("extractionId"));
            case MIGRATION_COMPLETE:
                return String.format("Migration %s completed", data.get("migrationId"));
            case SYSTEM_ALERT:
                return String.format("System Alert: %s", data.get("message"));
            default:
                return String.format("JiVS Notification: %s", data.get("message"));
        }
    }

    private String generateExtractionCompleteEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                    .button { background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Extraction Complete</h1>
                    </div>
                    <div class="content">
                        <p>Your data extraction has completed successfully.</p>
                        <p><strong>Extraction ID:</strong> %s</p>
                        <p><strong>Records Extracted:</strong> %s</p>
                        <p><strong>Duration:</strong> %s</p>
                        <p><a href="%s" class="button">View Details</a></p>
                    </div>
                    <div class="footer">
                        <p>JiVS Platform - Data Integration & Migration</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            data.get("extractionId"),
            data.get("recordCount"),
            data.get("duration"),
            data.get("detailsUrl")
        );
    }

    private String generateExtractionFailedEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f44336; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .error { background-color: #ffebee; padding: 10px; border-left: 4px solid #f44336; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Extraction Failed</h1>
                    </div>
                    <div class="content">
                        <p>Your data extraction has failed.</p>
                        <p><strong>Extraction ID:</strong> %s</p>
                        <div class="error">
                            <p><strong>Error:</strong> %s</p>
                        </div>
                        <p>Please review the error and try again or contact support.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            data.get("extractionId"),
            data.get("error")
        );
    }

    private String generateMigrationCompleteEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Migration Complete</h2>
                <p>Migration ID: %s</p>
                <p>Records Migrated: %s</p>
                <p>Status: SUCCESS</p>
            </body>
            </html>
            """,
            data.get("migrationId"),
            data.get("recordCount")
        );
    }

    private String generateMigrationFailedEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Migration Failed</h2>
                <p>Migration ID: %s</p>
                <p>Error: %s</p>
            </body>
            </html>
            """,
            data.get("migrationId"),
            data.get("error")
        );
    }

    private String generateDataQualityAlertEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Data Quality Alert</h2>
                <p>Rule: %s</p>
                <p>Severity: %s</p>
                <p>Description: %s</p>
            </body>
            </html>
            """,
            data.get("rule"),
            data.get("severity"),
            data.get("description")
        );
    }

    private String generateRetentionActionEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Retention Action Due</h2>
                <p>Policy: %s</p>
                <p>Action: %s</p>
                <p>Records Affected: %s</p>
                <p>Due Date: %s</p>
            </body>
            </html>
            """,
            data.get("policy"),
            data.get("action"),
            data.get("recordCount"),
            data.get("dueDate")
        );
    }

    private String generateComplianceRequestEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Compliance Request</h2>
                <p>Type: %s</p>
                <p>Subject: %s</p>
                <p>Due Date: %s</p>
                <p>Please review and process this request.</p>
            </body>
            </html>
            """,
            data.get("requestType"),
            data.get("subject"),
            data.get("dueDate")
        );
    }

    private String generateSystemAlertEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>System Alert</h2>
                <p>%s</p>
            </body>
            </html>
            """,
            data.get("message")
        );
    }

    private String generateApprovalRequiredEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Approval Required</h2>
                <p>Item: %s</p>
                <p>Requested by: %s</p>
                <p>Please review and approve or reject this request.</p>
            </body>
            </html>
            """,
            data.get("item"),
            data.get("requestedBy")
        );
    }

    private String generateGenericEmail(Map<String, Object> data) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Notification</h2>
                <p>%s</p>
            </body>
            </html>
            """,
            data.get("message")
        );
    }
}
