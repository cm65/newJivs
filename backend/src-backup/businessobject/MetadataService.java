package com.jivs.platform.service.businessobject;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing business object metadata
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final ObjectMapper objectMapper;

    /**
     * Create metadata from request
     */
    public Map<String, Object> createMetadata(Map<String, Object> metadataRequest) {
        Map<String, Object> metadata = new HashMap<>(metadataRequest);

        // Add system metadata
        metadata.put("_created_at", LocalDateTime.now().toString());
        metadata.put("_version", 1);
        metadata.put("_schema_version", "1.0");

        // Validate metadata schema
        validateMetadata(metadata);

        return metadata;
    }

    /**
     * Update metadata
     */
    public Map<String, Object> updateMetadata(Map<String, Object> existing, Map<String, Object> updates) {
        Map<String, Object> updated = new HashMap<>(existing);
        updated.putAll(updates);

        // Update system metadata
        updated.put("_updated_at", LocalDateTime.now().toString());
        Integer currentVersion = (Integer) updated.getOrDefault("_version", 1);
        updated.put("_version", currentVersion + 1);

        // Validate updated metadata
        validateMetadata(updated);

        return updated;
    }

    /**
     * Clone metadata
     */
    public Map<String, Object> cloneMetadata(Map<String, Object> source) {
        Map<String, Object> cloned = new HashMap<>(source);

        // Reset system metadata
        cloned.put("_created_at", LocalDateTime.now().toString());
        cloned.remove("_updated_at");
        cloned.put("_version", 1);
        cloned.put("_cloned_from", source.get("id"));

        return cloned;
    }

    /**
     * Validate metadata against schema
     */
    private void validateMetadata(Map<String, Object> metadata) {
        // Validate required fields
        if (metadata.isEmpty()) {
            throw new IllegalArgumentException("Metadata cannot be empty");
        }

        // Validate data types
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getValue() != null && !isValidMetadataValue(entry.getValue())) {
                throw new IllegalArgumentException(
                    "Invalid metadata value type for key: " + entry.getKey()
                );
            }
        }
    }

    /**
     * Check if value is valid for metadata
     */
    private boolean isValidMetadataValue(Object value) {
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Map ||
               value instanceof List ||
               value instanceof Date;
    }
}
