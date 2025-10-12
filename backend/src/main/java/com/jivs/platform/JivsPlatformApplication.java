package com.jivs.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for JiVS Information Management Platform
 *
 * This platform provides comprehensive enterprise data management capabilities including:
 * - Data migration (any-to-any)
 * - Application retirement
 * - Data quality management
 * - Retention management
 * - GDPR/CCPA compliance
 * - Document archiving
 * - Search and analytics
 *
 * @author JiVS Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class JivsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(JivsPlatformApplication.class, args);
    }
}
