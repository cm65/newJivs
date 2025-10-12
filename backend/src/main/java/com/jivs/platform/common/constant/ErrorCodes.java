package com.jivs.platform.common.constant;

/**
 * Error codes for different types of errors
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Authentication & Authorization (1xxx)
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH-1001";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH-1002";
    public static final String AUTH_TOKEN_INVALID = "AUTH-1003";
    public static final String AUTH_INSUFFICIENT_PERMISSIONS = "AUTH-1004";
    public static final String AUTH_ACCOUNT_LOCKED = "AUTH-1005";
    public static final String AUTH_ACCOUNT_DISABLED = "AUTH-1006";

    // User Management (2xxx)
    public static final String USER_NOT_FOUND = "USER-2001";
    public static final String USER_ALREADY_EXISTS = "USER-2002";
    public static final String USER_EMAIL_ALREADY_EXISTS = "USER-2003";
    public static final String USER_INVALID_PASSWORD = "USER-2004";

    // Extraction (3xxx)
    public static final String EXTRACTION_JOB_NOT_FOUND = "EXTRACT-3001";
    public static final String EXTRACTION_CONFIG_INVALID = "EXTRACT-3002";
    public static final String EXTRACTION_CONNECTION_FAILED = "EXTRACT-3003";
    public static final String EXTRACTION_ALREADY_RUNNING = "EXTRACT-3004";

    // Migration (4xxx)
    public static final String MIGRATION_PROJECT_NOT_FOUND = "MIGRATE-4001";
    public static final String MIGRATION_JOB_NOT_FOUND = "MIGRATE-4002";
    public static final String MIGRATION_DEPENDENCY_NOT_MET = "MIGRATE-4003";
    public static final String MIGRATION_ALREADY_RUNNING = "MIGRATE-4004";

    // Data Quality (5xxx)
    public static final String DQ_RULE_NOT_FOUND = "DQ-5001";
    public static final String DQ_CHECK_FAILED = "DQ-5002";
    public static final String DQ_VALIDATION_FAILED = "DQ-5003";

    // Business Objects (6xxx)
    public static final String BO_NOT_FOUND = "BO-6001";
    public static final String BO_ALREADY_EXISTS = "BO-6002";
    public static final String BO_INVALID_SCHEMA = "BO-6003";

    // Documents (7xxx)
    public static final String DOC_NOT_FOUND = "DOC-7001";
    public static final String DOC_UPLOAD_FAILED = "DOC-7002";
    public static final String DOC_INVALID_FORMAT = "DOC-7003";
    public static final String DOC_SIZE_EXCEEDED = "DOC-7004";

    // Compliance (8xxx)
    public static final String COMPLIANCE_REQUEST_NOT_FOUND = "COMP-8001";
    public static final String COMPLIANCE_POLICY_VIOLATION = "COMP-8002";
    public static final String COMPLIANCE_VERIFICATION_FAILED = "COMP-8003";

    // System (9xxx)
    public static final String SYSTEM_ERROR = "SYS-9001";
    public static final String SYSTEM_CONFIGURATION_ERROR = "SYS-9002";
    public static final String SYSTEM_DATABASE_ERROR = "SYS-9003";
    public static final String SYSTEM_EXTERNAL_SERVICE_ERROR = "SYS-9004";
}
