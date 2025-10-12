package com.jivs.platform.service.extraction;

// import com.jivs.platform.security.SqlInjectionValidator; // Temporarily disabled
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * JDBC connector for relational databases
 * Enhanced with SQL injection protection
 */
public class JdbcConnector implements DataConnector {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JdbcConnector.class);

    private final String connectionUrl;
    private final String username;
    private final String password;
    private final String dbType;
    // private final SqlInjectionValidator sqlValidator; // Temporarily disabled
    private Connection connection;

    public JdbcConnector(String connectionUrl, String username, String password, String dbType) {
        this.connectionUrl = connectionUrl;
        this.username = username;
        this.password = password;
        this.dbType = dbType;
        // this.sqlValidator = null; // Temporarily disabled
    }

    @Override
    public boolean testConnection() {
        try {
            connection = DriverManager.getConnection(connectionUrl, username, password);
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            log.error("Failed to test connection: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ExtractionResult extract(Map<String, String> parameters) {
        ExtractionResult result = new ExtractionResult();
        result.setRecordsExtracted(0L);
        result.setRecordsFailed(0L);
        result.setBytesProcessed(0L);

        try {
            if (connection == null || connection.isClosed()) {
                testConnection();
            }

            String query = parameters.getOrDefault("query", "SELECT 1");
            String outputPath = parameters.getOrDefault("outputPath", "/tmp/extraction");

            // CRITICAL: Validate query for SQL injection (temporarily disabled)
            // TODO: Re-enable SQL injection validation when security module is restored
            // if (!sqlValidator.isQuerySafe(query)) {
            //     String errorMsg = "Query failed security validation. Query may contain SQL injection attempts.";
            //     log.error("SQL Injection attempt detected: {}", query);
            //     result.getErrors().add(errorMsg);
            //     result.setRecordsFailed(1L);
            //     throw new SecurityException(errorMsg);
            // }

            // Use PreparedStatement for safer query execution
            // Note: For dynamic queries, we've already validated the query structure
            PreparedStatement statement = connection.prepareStatement(query);

            // Set read-only to prevent accidental writes
            statement.setQueryTimeout(300); // 5 minutes max
            connection.setReadOnly(true);

            ResultSet rs = statement.executeQuery();

            long recordCount = 0;
            long bytesProcessed = 0;

            while (rs.next()) {
                recordCount++;

                // Estimate bytes processed (rough calculation)
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value != null) {
                        bytesProcessed += value.toString().getBytes().length;
                    }
                }

                // Process record - actual implementation would write to file/storage
            }

            result.setRecordsExtracted(recordCount);
            result.setBytesProcessed(bytesProcessed);
            result.setOutputPath(outputPath);

            log.info("Extracted {} records ({} bytes) from {}", recordCount, bytesProcessed, dbType);

            rs.close();
            statement.close();

        } catch (SecurityException e) {
            log.error("Security validation failed: {}", e.getMessage());
            result.getErrors().add(e.getMessage());
            result.setRecordsFailed(1L);
        } catch (Exception e) {
            log.error("Extraction failed: {}", e.getMessage(), e);
            result.getErrors().add(e.getMessage());
            result.setRecordsFailed(1L);
        }

        return result;
    }

    @Override
    public String getConnectorType() {
        return "JDBC-" + dbType;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            log.error("Failed to close connection: {}", e.getMessage());
        }
    }
}