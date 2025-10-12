package com.jivs.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.jivs.platform.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String[] elasticsearchUris;

    // Spring Boot 3.x auto-configures Elasticsearch client
    // Additional custom configuration can be added here if needed
}
