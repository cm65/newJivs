package com.jivs.platform.security;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for preventing SQL injection attacks
 * Analyzes SQL queries for malicious patterns
 */
@Component
public class SqlInjectionValidator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SqlInjectionValidator.class);

    // Dangerous SQL keywords that indicate potential injection
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE",
        "INSERT", "UPDATE", "EXEC", "EXECUTE", "GRANT",
        "REVOKE", "SHUTDOWN", "DECLARE", "CAST", "CONVERT",
        "BULK", "BACKUP", "RESTORE", "MERGE", "DBCC"
    );

    // Dangerous patterns
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        Pattern.compile("--", Pattern.CASE_INSENSITIVE),              // SQL comments
        Pattern.compile("/\\*.*?\\*/", Pattern.CASE_INSENSITIVE),     // Multi-line comments
        Pattern.compile(";\\s*(DROP|DELETE|UPDATE|INSERT|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE)",
            Pattern.CASE_INSENSITIVE),                                 // Multiple statements
        Pattern.compile("(UNION|UNION\\s+ALL)\\s+SELECT",
            Pattern.CASE_INSENSITIVE),                                 // UNION-based injection
        Pattern.compile("'\\s*(OR|AND)\\s+'?1'?\\s*=\\s*'?1",
            Pattern.CASE_INSENSITIVE),                                 // Tautology attacks
        Pattern.compile("(\\bOR\\b|\\bAND\\b)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?",
            Pattern.CASE_INSENSITIVE),                                 // Boolean-based attacks
        Pattern.compile("(\\bOR\\b|\\bAND\\b)\\s+['\"]?[a-zA-Z]+['\"]?\\s*=\\s*['\"]?[a-zA-Z]+['\"]?",
            Pattern.CASE_INSENSITIVE),                                 // String-based attacks
        Pattern.compile("xp_", Pattern.CASE_INSENSITIVE),             // SQL Server extended procedures
        Pattern.compile("sp_", Pattern.CASE_INSENSITIVE),             // SQL Server stored procedures
        Pattern.compile("0x[0-9a-fA-F]+", Pattern.CASE_INSENSITIVE), // Hex encoding
        Pattern.compile("WAITFOR\\s+DELAY", Pattern.CASE_INSENSITIVE), // Time-based attacks
        Pattern.compile("BENCHMARK\\s*\\(", Pattern.CASE_INSENSITIVE), // MySQL time-based attacks
        Pattern.compile("SLEEP\\s*\\(", Pattern.CASE_INSENSITIVE),    // MySQL/PostgreSQL sleep
        Pattern.compile("PG_SLEEP\\s*\\(", Pattern.CASE_INSENSITIVE)  // PostgreSQL sleep
    );

    /**
     * Validate SQL query for injection attempts
     *
     * @param query The SQL query to validate
     * @return true if query is safe, false if potential injection detected
     */
    public boolean isQuerySafe(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String upperQuery = query.toUpperCase().trim();

        // Check if query starts with SELECT (read-only)
        if (!upperQuery.startsWith("SELECT")) {
            log.warn("Query does not start with SELECT: {}", query);
            return false;
        }

        // Check for dangerous keywords
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperQuery.contains(keyword)) {
                log.warn("Query contains dangerous keyword {}: {}", keyword, query);
                return false;
            }
        }

        // Check for dangerous patterns
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(query).find()) {
                log.warn("Query matches dangerous pattern {}: {}", pattern.pattern(), query);
                return false;
            }
        }

        // Check for excessive UNION clauses (might indicate injection)
        long unionCount = Arrays.stream(upperQuery.split("\\s+"))
            .filter(word -> word.equals("UNION"))
            .count();
        if (unionCount > 2) {
            log.warn("Query contains too many UNION clauses: {}", query);
            return false;
        }

        return true;
    }

    /**
     * Sanitize table/column names for dynamic SQL
     * Only allows alphanumeric characters and underscores
     *
     * @param identifier Table or column name
     * @return Sanitized identifier
     * @throws IllegalArgumentException if identifier is invalid
     */
    public String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        // Only allow alphanumeric and underscore
        if (!identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            log.error("Invalid identifier: {}", identifier);
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }

        // Check length
        if (identifier.length() > 64) {
            throw new IllegalArgumentException("Identifier too long: " + identifier);
        }

        // Check for SQL keywords used as identifiers
        if (DANGEROUS_KEYWORDS.contains(identifier.toUpperCase())) {
            throw new IllegalArgumentException("Identifier cannot be SQL keyword: " + identifier);
        }

        return identifier;
    }

    /**
     * Validate and sanitize a list of column names
     *
     * @param columns List of column names
     * @return Sanitized column names joined with commas
     * @throws IllegalArgumentException if any column name is invalid
     */
    public String sanitizeColumns(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Column list cannot be null or empty");
        }

        return columns.stream()
            .map(this::sanitizeIdentifier)
            .reduce((a, b) -> a + ", " + b)
            .orElse("*");
    }

    /**
     * Validate ORDER BY clause to prevent injection
     *
     * @param orderBy The ORDER BY clause
     * @return true if safe
     */
    public boolean isOrderBySafe(String orderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return true;
        }

        // ORDER BY should only contain: column names, ASC/DESC, commas, and spaces
        if (!orderBy.matches("^[a-zA-Z0-9_,\\s]+(ASC|DESC)?$")) {
            log.warn("Potentially unsafe ORDER BY clause: {}", orderBy);
            return false;
        }

        return true;
    }

    /**
     * Create a safe query builder that uses parameterized queries
     */
    public static class SafeQueryBuilder {
        private final StringBuilder query = new StringBuilder();
        private final List<Object> parameters = new java.util.ArrayList<>();

        public SafeQueryBuilder select(String... columns) {
            query.append("SELECT ");
            if (columns.length == 0) {
                query.append("*");
            } else {
                query.append(String.join(", ", columns));
            }
            return this;
        }

        public SafeQueryBuilder from(String table) {
            query.append(" FROM ").append(table);
            return this;
        }

        public SafeQueryBuilder where(String condition) {
            query.append(" WHERE ").append(condition);
            return this;
        }

        public SafeQueryBuilder and(String condition) {
            query.append(" AND ").append(condition);
            return this;
        }

        public SafeQueryBuilder or(String condition) {
            query.append(" OR ").append(condition);
            return this;
        }

        public SafeQueryBuilder orderBy(String column, String direction) {
            query.append(" ORDER BY ").append(column).append(" ").append(direction);
            return this;
        }

        public SafeQueryBuilder limit(int limit) {
            query.append(" LIMIT ?");
            parameters.add(limit);
            return this;
        }

        public SafeQueryBuilder offset(int offset) {
            query.append(" OFFSET ?");
            parameters.add(offset);
            return this;
        }

        public String build() {
            return query.toString();
        }

        public List<Object> getParameters() {
            return parameters;
        }
    }

    /**
     * Escape special characters in LIKE patterns
     *
     * @param pattern The LIKE pattern
     * @return Escaped pattern
     */
    public String escapeLikePattern(String pattern) {
        if (pattern == null) {
            return null;
        }

        // Escape special LIKE characters: %, _, [, ]
        return pattern.replace("\\", "\\\\")
                     .replace("%", "\\%")
                     .replace("_", "\\_")
                     .replace("[", "\\[")
                     .replace("]", "\\]");
    }
}
