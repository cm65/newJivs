package com.jivs.platform.service.extraction;

import java.util.Map;

/**
 * Interface for data connectors
 */
public interface DataConnector {

    /**
     * Test connection to data source
     */
    boolean testConnection();

    /**
     * Extract data from source
     */
    ExtractionResult extract(Map<String, String> parameters);

    /**
     * Get connector type
     */
    String getConnectorType();

    /**
     * Close connection
     */
    void close();
}