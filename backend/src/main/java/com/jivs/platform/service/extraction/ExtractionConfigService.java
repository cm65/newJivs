package com.jivs.platform.service.extraction;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.exception.ResourceNotFoundException;
import com.jivs.platform.domain.extraction.DataSource;
import com.jivs.platform.domain.extraction.ExtractionConfig;
import com.jivs.platform.repository.DataSourceRepository;
import com.jivs.platform.repository.ExtractionConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for managing extraction configurations (CRUD operations for UI)
 */
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractionConfigService.class);

    private final ExtractionConfigRepository extractionConfigRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractionService extractionService;

    /**
     * Create a new extraction configuration
     * This is called from the UI when user clicks "New Extraction"
     */
    @Transactional
    public ExtractionConfig createExtractionConfig(Map<String, Object> request, String createdBy) {
        String name = (String) request.get("name");
        String sourceType = (String) request.get("sourceType");
        String extractionQuery = (String) request.get("extractionQuery");
        Map<String, String> connectionConfig = (Map<String, String>) request.get("connectionConfig");

        log.info("Creating extraction config: {}", name);

        // Check if name already exists
        if (extractionConfigRepository.findByName(name).isPresent()) {
            throw new BusinessException("Extraction config with name '" + name + "' already exists");
        }

        // Find or create data source for this config
        DataSource dataSource = findOrCreateDataSource(sourceType, connectionConfig, createdBy);

        // Create extraction config
        ExtractionConfig config = new ExtractionConfig();
        config.setName(name);
        config.setDataSource(dataSource);
        config.setExtractionType("FULL"); // Default type
        config.setExtractionQuery(extractionQuery);
        config.setIsEnabled(true);
        config.setCreatedBy(createdBy);

        ExtractionConfig savedConfig = extractionConfigRepository.save(config);
        log.info("Extraction config created with ID: {}", savedConfig.getId());

        return savedConfig;
    }

    /**
     * Get extraction config by ID
     */
    public ExtractionConfig getExtractionConfig(Long id) {
        return extractionConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionConfig", "id", id));
    }

    /**
     * Get all extraction configs with pagination
     */
    public Page<ExtractionConfig> getAllExtractionConfigs(Pageable pageable) {
        return extractionConfigRepository.findAll(pageable);
    }

    /**
     * Update extraction config
     */
    @Transactional
    public ExtractionConfig updateExtractionConfig(Long id, Map<String, Object> updates, String updatedBy) {
        log.info("Updating extraction config: {}", id);

        ExtractionConfig config = getExtractionConfig(id);

        if (updates.containsKey("name")) {
            config.setName((String) updates.get("name"));
        }
        if (updates.containsKey("extractionQuery")) {
            config.setExtractionQuery((String) updates.get("extractionQuery"));
        }
        if (updates.containsKey("isEnabled")) {
            config.setIsEnabled((Boolean) updates.get("isEnabled"));
        }

        config.setUpdatedBy(updatedBy);

        return extractionConfigRepository.save(config);
    }

    /**
     * Delete extraction config
     */
    @Transactional
    public void deleteExtractionConfig(Long id) {
        log.info("Deleting extraction config: {}", id);

        ExtractionConfig config = getExtractionConfig(id);
        extractionConfigRepository.delete(config);
    }

    /**
     * Start extraction job for a config
     */
    @Transactional
    public void startExtraction(Long configId, String triggeredBy) {
        log.info("Starting extraction for config: {}", configId);

        ExtractionConfig config = getExtractionConfig(configId);

        if (!config.getIsEnabled()) {
            throw new BusinessException("Extraction config is disabled");
        }

        // Create and queue extraction job
        extractionService.createExtractionJob(
                config.getDataSource().getId(),
                Map.of("configId", configId.toString(), "query", config.getExtractionQuery() != null ? config.getExtractionQuery() : ""),
                triggeredBy
        );
    }

    /**
     * Find or create data source for extraction config
     */
    private DataSource findOrCreateDataSource(String sourceType, Map<String, String> connectionConfig, String createdBy) {
        // Try to find existing data source with same connection
        String connectionUrl = connectionConfig != null ? connectionConfig.get("url") : null;

        if (connectionUrl != null) {
            // Check if data source already exists
            var existingSource = dataSourceRepository.findAll().stream()
                    .filter(ds -> ds.getConnectionUrl() != null && ds.getConnectionUrl().equals(connectionUrl))
                    .findFirst();

            if (existingSource.isPresent()) {
                return existingSource.get();
            }
        }

        // Create new data source
        DataSource dataSource = new DataSource();
        dataSource.setName(sourceType + " Source - " + System.currentTimeMillis());
        dataSource.setSourceType(DataSource.SourceType.valueOf(sourceType.toUpperCase()));
        dataSource.setConnectionUrl(connectionUrl);

        if (connectionConfig != null) {
            dataSource.setUsername(connectionConfig.get("username"));
            dataSource.setPasswordEncrypted(connectionConfig.get("password")); // TODO: Encrypt properly
        }

        dataSource.setIsActive(true);
        dataSource.setCreatedBy(createdBy);

        return dataSourceRepository.save(dataSource);
    }
}
