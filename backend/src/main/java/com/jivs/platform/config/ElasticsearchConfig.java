package com.jivs.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration
 * Only loaded in non-production profiles where Elasticsearch is available
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.jivs.platform.repository")
@Profile("!production")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String[] elasticsearchUris;

    // Spring Boot 3.x auto-configures Elasticsearch client
    // Additional custom configuration can be added here if needed
}
