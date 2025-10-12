package com.jivs.platform.domain.extraction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Source entity for connection configurations
 */
@Entity
@Table(name = "data_sources", indexes = {
        @Index(name = "idx_data_sources_type", columnList = "source_type"),
        @Index(name = "idx_data_sources_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "connection_url", length = 500)
    private String connectionUrl;

    @Column
    private String host;

    @Column
    private Integer port;

    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(length = 100)
    private String username;

    @Column(name = "password_encrypted", length = 500)
    private String passwordEncrypted;

    @ElementCollection
    @CollectionTable(name = "data_source_properties",
                     joinColumns = @JoinColumn(name = "data_source_id"))
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    private Map<String, String> additionalProperties = new HashMap<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_connection_test")
    private LocalDateTime lastConnectionTest;

    @Column(name = "last_connection_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ConnectionStatus lastConnectionStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(String passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastConnectionTest() {
        return lastConnectionTest;
    }

    public void setLastConnectionTest(LocalDateTime lastConnectionTest) {
        this.lastConnectionTest = lastConnectionTest;
    }

    public ConnectionStatus getLastConnectionStatus() {
        return lastConnectionStatus;
    }

    public void setLastConnectionStatus(ConnectionStatus lastConnectionStatus) {
        this.lastConnectionStatus = lastConnectionStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public enum SourceType {
        SAP, ORACLE, SQL_SERVER, POSTGRESQL, MYSQL, FILE, API
    }

    public enum ConnectionStatus {
        SUCCESS, FAILED, UNTESTED
    }
}