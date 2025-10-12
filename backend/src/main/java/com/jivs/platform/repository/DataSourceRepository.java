package com.jivs.platform.repository;

import com.jivs.platform.domain.extraction.DataSource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * P0.3: Repository for DataSource entity with Redis caching
 *
 * Caching Strategy:
 * - DataSource lookups cached for 1 hour (rarely change)
 * - Cache evicted on save/delete operations
 * - Cache hit rate target: 70%+
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    /**
     * Find by ID with caching (1 hour TTL)
     */
    @Cacheable(value = "dataSources", key = "#id")
    @Override
    Optional<DataSource> findById(Long id);

    /**
     * Find by source type with caching
     */
    @Cacheable(value = "dataSources", key = "'type:' + #sourceType.name()")
    List<DataSource> findBySourceType(DataSource.SourceType sourceType);

    /**
     * Find active data sources with caching
     */
    @Cacheable(value = "dataSources", key = "'active'")
    List<DataSource> findByIsActiveTrue();

    Page<DataSource> findByIsActive(Boolean isActive, Pageable pageable);

    List<DataSource> findByLastConnectionStatus(DataSource.ConnectionStatus status);

    /**
     * Save with cache eviction
     */
    @Caching(evict = {
        @CacheEvict(value = "dataSources", key = "#entity.id"),
        @CacheEvict(value = "dataSources", key = "'type:' + #entity.sourceType.name()"),
        @CacheEvict(value = "dataSources", key = "'active'"),
        @CacheEvict(value = "connectionPools", key = "#entity.id")
    })
    @Override
    <S extends DataSource> S save(S entity);

    /**
     * Delete with cache eviction
     */
    @Caching(evict = {
        @CacheEvict(value = "dataSources", key = "#id"),
        @CacheEvict(value = "dataSources", allEntries = true),
        @CacheEvict(value = "connectionPools", key = "#id")
    })
    @Override
    void deleteById(Long id);
}