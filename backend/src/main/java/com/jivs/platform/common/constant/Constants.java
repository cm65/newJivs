package com.jivs.platform.common.constant;

/**
 * Application-wide constants
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // API Version
    public static final String API_V1 = "/api/v1";

    // Date/Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Pagination Defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // System User
    public static final String SYSTEM_USER = "SYSTEM";
    public static final String ANONYMOUS_USER = "ANONYMOUS";

    // Security
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int TOKEN_BEGIN_INDEX = 7;

    // Cache Keys
    public static final String CACHE_USERS = "users";
    public static final String CACHE_ROLES = "roles";
    public static final String CACHE_BUSINESS_OBJECTS = "business_objects";
    public static final String CACHE_DATA_SOURCES = "data_sources";

    // Job Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // File Extensions
    public static final String[] DOCUMENT_EXTENSIONS = {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "xml", "json"
    };

    public static final String[] IMAGE_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "svg"
    };

    // Max Sizes
    public static final long MAX_FILE_SIZE_MB = 100;
    public static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

    // Batch Sizes
    public static final int DEFAULT_BATCH_SIZE = 1000;
    public static final int MAX_BATCH_SIZE = 10000;

    // Retry Configuration
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_RETRY_DELAY_MS = 5000;

    // Data Quality Thresholds
    public static final double DEFAULT_QUALITY_THRESHOLD = 0.95;
    public static final double DEFAULT_DUPLICATE_THRESHOLD = 0.85;

    // Compliance
    public static final int GDPR_DATA_SUBJECT_REQUEST_DAYS = 30;
    public static final int DEFAULT_RETENTION_YEARS = 7;
    public static final int AUDIT_LOG_RETENTION_YEARS = 7;
}
