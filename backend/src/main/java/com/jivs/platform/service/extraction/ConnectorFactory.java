package com.jivs.platform.service.extraction;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.util.CryptoUtil;
import com.jivs.platform.domain.extraction.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * P0.2: Factory for creating data connectors with connection pooling
 *
 * Changes:
 * - JDBC connectors now use connection pooling for better performance
 * - Pool reuse eliminates connection creation overhead
 */
@Component
@RequiredArgsConstructor
public class ConnectorFactory {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorFactory.class);

    private final CryptoUtil cryptoUtil;
    private final ExtractionDataSourcePool dataSourcePool;

    /**
     * Get connector for data source with connection pooling
     */
    public DataConnector getConnector(DataSource dataSource) {
        log.debug("Creating connector for data source type: {}", dataSource.getSourceType());

        switch (dataSource.getSourceType()) {
            case POSTGRESQL:
            case MYSQL:
            case ORACLE:
            case SQL_SERVER:
                // P0.2: Use pooled JDBC connector for better performance
                return new PooledJdbcConnector(dataSourcePool, dataSource);

            case SAP:
                return new SapConnector(
                        dataSource.getHost(),
                        dataSource.getAdditionalProperties()
                );

            case FILE:
                return new FileConnector(
                        dataSource.getAdditionalProperties()
                );

            case API:
                return new ApiConnector(
                        dataSource.getConnectionUrl(),
                        dataSource.getAdditionalProperties()
                );

            default:
                throw new BusinessException("Unsupported data source type: " + dataSource.getSourceType());
        }
    }

    /**
     * Get legacy (non-pooled) connector - kept for backward compatibility
     * Use this only if pooled connector causes issues
     */
    @Deprecated
    public DataConnector getLegacyConnector(DataSource dataSource) {
        log.warn("Using legacy non-pooled connector for data source: {}", dataSource.getName());

        String decryptedPassword = null;
        if (dataSource.getPasswordEncrypted() != null) {
            decryptedPassword = cryptoUtil.decrypt(dataSource.getPasswordEncrypted());
        }

        return new JdbcConnector(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword,
                dataSource.getSourceType().name()
        );
    }
}