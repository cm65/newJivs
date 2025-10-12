package com.jivs.platform.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing user inputs to prevent XSS attacks
 * Implements OWASP XSS prevention guidelines
 */
@Component
public class XssSanitizer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XssSanitizer.class);

    // Patterns for detecting XSS attempts
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
        "on(load|error|click|mouse|focus|blur|change|submit)[^=]*=",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IFRAME_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "<object[^>]*>.*?</object>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern EMBED_PATTERN = Pattern.compile(
        "<embed[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern META_PATTERN = Pattern.compile(
        "<meta[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<link[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern STYLE_PATTERN = Pattern.compile(
        "<style[^>]*>.*?</style>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern BASE_PATTERN = Pattern.compile(
        "<base[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "@import",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile(
        "vbscript:",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATA_PATTERN = Pattern.compile(
        "data:text/html",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize user input by removing potentially malicious content
     *
     * @param input The user input to sanitize
     * @return Sanitized input
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove vbscript: protocol
        sanitized = VBSCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove data:text/html
        sanitized = DATA_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers
        sanitized = ON_EVENT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove iframe tags
        sanitized = IFRAME_PATTERN.matcher(sanitized).replaceAll("");

        // Remove object tags
        sanitized = OBJECT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove embed tags
        sanitized = EMBED_PATTERN.matcher(sanitized).replaceAll("");

        // Remove meta tags
        sanitized = META_PATTERN.matcher(sanitized).replaceAll("");

        // Remove link tags
        sanitized = LINK_PATTERN.matcher(sanitized).replaceAll("");

        // Remove style tags
        sanitized = STYLE_PATTERN.matcher(sanitized).replaceAll("");

        // Remove base tags
        sanitized = BASE_PATTERN.matcher(sanitized).replaceAll("");

        // Remove @import in CSS
        sanitized = IMPORT_PATTERN.matcher(sanitized).replaceAll("");

        // Encode HTML special characters
        sanitized = encodeHtmlEntities(sanitized);

        if (!sanitized.equals(input)) {
            log.warn("XSS attempt detected and sanitized. Original length: {}, Sanitized length: {}",
                input.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Encode HTML special characters to prevent XSS
     *
     * @param input The input to encode
     * @return HTML-encoded string
     */
    public String encodeHtmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    /**
     * Decode HTML entities back to characters
     *
     * @param input The encoded input
     * @return Decoded string
     */
    public String decodeHtmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/");
    }

    /**
     * Check if input contains potential XSS attack vectors
     *
     * @param input The input to check
     * @return true if XSS detected, false otherwise
     */
    public boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        return SCRIPT_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(input).find() ||
               VBSCRIPT_PATTERN.matcher(input).find() ||
               ON_EVENT_PATTERN.matcher(input).find() ||
               IFRAME_PATTERN.matcher(input).find() ||
               OBJECT_PATTERN.matcher(input).find() ||
               EMBED_PATTERN.matcher(input).find() ||
               META_PATTERN.matcher(input).find() ||
               lowerInput.contains("<script") ||
               lowerInput.contains("javascript:") ||
               lowerInput.contains("onerror=") ||
               lowerInput.contains("onload=");
    }

    /**
     * Sanitize filename to prevent path traversal attacks
     *
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        // Remove path traversal sequences
        String sanitized = filename.replace("../", "")
                                  .replace("..\\", "")
                                  .replace("./", "")
                                  .replace(".\\", "");

        // Remove path separators
        sanitized = sanitized.replace("/", "_")
                            .replace("\\", "_");

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        // Remove control characters
        sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "");

        // Only allow alphanumeric, dash, underscore, and dot
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Prevent multiple dots (potential double extension attack)
        sanitized = sanitized.replaceAll("\\.{2,}", ".");

        // Limit length
        if (sanitized.length() > 255) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, 255 - extension.length()) + extension;
            } else {
                sanitized = sanitized.substring(0, 255);
            }
        }

        return sanitized;
    }

    /**
     * Validate that string only contains safe characters (alphanumeric + basic punctuation)
     *
     * @param input The input to validate
     * @return true if safe, false otherwise
     */
    public boolean isSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // Allow alphanumeric, space, and basic punctuation
        return input.matches("^[a-zA-Z0-9\\s.,;:!?()\\[\\]{}\"'\\-_@#$%&*+=<>]*$");
    }

    /**
     * Strip all HTML tags from input
     *
     * @param input The input containing HTML
     * @return Plain text without HTML tags
     */
    public String stripHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove all HTML tags
        String stripped = input.replaceAll("<[^>]+>", "");

        // Decode HTML entities
        stripped = decodeHtmlEntities(stripped);

        return stripped.trim();
    }

    /**
     * Sanitize URL to prevent XSS in href attributes
     *
     * @param url The URL to sanitize
     * @return Sanitized URL or empty string if unsafe
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String lowerUrl = url.toLowerCase().trim();

        // Only allow http, https, and mailto protocols
        if (!lowerUrl.startsWith("http://") &&
            !lowerUrl.startsWith("https://") &&
            !lowerUrl.startsWith("mailto:") &&
            !lowerUrl.startsWith("/")) {
            log.warn("Unsafe URL protocol detected: {}", url);
            return "";
        }

        // Check for javascript: or data: in URL
        if (lowerUrl.contains("javascript:") ||
            lowerUrl.contains("vbscript:") ||
            lowerUrl.contains("data:")) {
            log.warn("XSS attempt in URL: {}", url);
            return "";
        }

        return url;
    }
}
