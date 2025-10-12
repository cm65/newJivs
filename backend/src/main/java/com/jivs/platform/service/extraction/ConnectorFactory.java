package com.jivs.platform.service.extraction;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.util.CryptoUtil;
import com.jivs.platform.domain.extraction.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating data connectors
 */
@Component
@RequiredArgsConstructor
public class ConnectorFactory {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorFactory.class);

    private final CryptoUtil cryptoUtil;

    /**
     * Get connector for data source
     */
    public DataConnector getConnector(DataSource dataSource) {
        log.debug("Creating connector for data source type: {}", dataSource.getSourceType());

        String decryptedPassword = null;
        if (dataSource.getPasswordEncrypted() != null) {
            decryptedPassword = cryptoUtil.decrypt(dataSource.getPasswordEncrypted());
        }

        switch (dataSource.getSourceType()) {
            case POSTGRESQL:
            case MYSQL:
            case ORACLE:
            case SQL_SERVER:
                return new JdbcConnector(
                        dataSource.getConnectionUrl(),
                        dataSource.getUsername(),
                        decryptedPassword,
                        dataSource.getSourceType().name()
                );

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
}