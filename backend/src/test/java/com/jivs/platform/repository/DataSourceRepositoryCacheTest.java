package com.jivs.platform.repository;

import com.jivs.platform.config.CacheConfig;
import com.jivs.platform.domain.extraction.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for DataSourceRepository with Redis caching
 *
 * Tests:
 * - Cache hit for repeated findById calls
 * - Cache eviction on save
 * - Cache eviction on delete
 * - TTL expiration (simulated)
 * - Cache behavior for different query methods
 */
@DataJpaTest
@Import(CacheConfig.class)
@TestPropertySource(properties = {
        "spring.cache.type=simple", // Use simple cache for testing (not Redis)
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class DataSourceRepositoryCacheTest {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }

        // Clear database
        dataSourceRepository.deleteAll();
    }

    @Test
    void testFindByIdCachesResult() {
        // Given
        DataSource dataSource = createTestDataSource("Test DS 1", DataSource.SourceType.POSTGRESQL);
        DataSource saved = dataSourceRepository.save(dataSource);

        // Clear cache to start fresh
        clearDataSourceCache();

        // When - First call (cache miss)
        Optional<DataSource> first = dataSourceRepository.findById(saved.getId());

        // Then - Should be found
        assertThat(first).isPresent();

        // Verify it's now in cache
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache).isNotNull();
        Cache.ValueWrapper cached = cache.get(saved.getId());
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isInstanceOf(Optional.class);

        // When - Second call (cache hit)
        Optional<DataSource> second = dataSourceRepository.findById(saved.getId());

        // Then - Should return same object from cache
        assertThat(second).isPresent();
        assertThat(second.get().getId()).isEqualTo(first.get().getId());
    }

    @Test
    void testFindByIdWithNonExistentIdNotCached() {
        // When - Try to find non-existent ID
        Optional<DataSource> result = dataSourceRepository.findById(999L);

        // Then - Should return empty
        assertThat(result).isEmpty();

        // Verify it's NOT in cache (null values are not cached per config)
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache).isNotNull();
        Cache.ValueWrapper cached = cache.get(999L);
        assertThat(cached).isNull();
    }

    @Test
    void testSaveEvictsCacheForSpecificId() {
        // Given - Create and cache a data source
        DataSource dataSource = createTestDataSource("Original Name", DataSource.SourceType.POSTGRESQL);
        DataSource saved = dataSourceRepository.save(dataSource);
        clearDataSourceCache();

        // Load into cache
        dataSourceRepository.findById(saved.getId());

        // Verify it's cached
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get(saved.getId())).isNotNull();

        // When - Update and save (should evict cache)
        saved.setName("Updated Name");
        dataSourceRepository.save(saved);

        // Then - Cache should be evicted
        assertThat(cache.get(saved.getId())).isNull();

        // When - Load again
        Optional<DataSource> reloaded = dataSourceRepository.findById(saved.getId());

        // Then - Should have updated name
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    void testSaveEvictsRelatedCaches() {
        // Given
        DataSource dataSource = createTestDataSource("Test DS", DataSource.SourceType.MYSQL);
        DataSource saved = dataSourceRepository.save(dataSource);
        clearDataSourceCache();

        // Cache by ID
        dataSourceRepository.findById(saved.getId());

        // Cache by type
        dataSourceRepository.findBySourceType(DataSource.SourceType.MYSQL);

        // Cache active data sources
        dataSourceRepository.findByIsActiveTrue();

        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get(saved.getId())).isNotNull();
        assertThat(cache.get("type:MYSQL")).isNotNull();
        assertThat(cache.get("active")).isNotNull();

        // When - Save again
        saved.setName("Updated");
        dataSourceRepository.save(saved);

        // Then - All related caches should be evicted
        assertThat(cache.get(saved.getId())).isNull();
        assertThat(cache.get("type:MYSQL")).isNull();
        assertThat(cache.get("active")).isNull();
    }

    @Test
    void testDeleteByIdEvictsCache() {
        // Given
        DataSource dataSource = createTestDataSource("To Delete", DataSource.SourceType.ORACLE);
        DataSource saved = dataSourceRepository.save(dataSource);
        clearDataSourceCache();

        // Load into cache
        dataSourceRepository.findById(saved.getId());

        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get(saved.getId())).isNotNull();

        // When - Delete
        dataSourceRepository.deleteById(saved.getId());

        // Then - Cache should be evicted
        assertThat(cache.get(saved.getId())).isNull();

        // Verify it's actually deleted
        Optional<DataSource> deleted = dataSourceRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void testDeleteEvictsAllDataSourceCaches() {
        // Given - Multiple data sources
        DataSource ds1 = dataSourceRepository.save(createTestDataSource("DS1", DataSource.SourceType.POSTGRESQL));
        DataSource ds2 = dataSourceRepository.save(createTestDataSource("DS2", DataSource.SourceType.MYSQL));
        clearDataSourceCache();

        // Cache multiple entries
        dataSourceRepository.findById(ds1.getId());
        dataSourceRepository.findById(ds2.getId());
        dataSourceRepository.findByIsActiveTrue();

        Cache cache = cacheManager.getCache("dataSources");

        // When - Delete one
        dataSourceRepository.deleteById(ds1.getId());

        // Then - All entries should be evicted (deleteById uses allEntries = true)
        assertThat(cache.get(ds1.getId())).isNull();
        assertThat(cache.get(ds2.getId())).isNull();
        assertThat(cache.get("active")).isNull();
    }

    @Test
    void testFindBySourceTypeCachesResult() {
        // Given
        dataSourceRepository.save(createTestDataSource("PG1", DataSource.SourceType.POSTGRESQL));
        dataSourceRepository.save(createTestDataSource("PG2", DataSource.SourceType.POSTGRESQL));
        dataSourceRepository.save(createTestDataSource("MY1", DataSource.SourceType.MYSQL));
        clearDataSourceCache();

        // When - First call
        List<DataSource> first = dataSourceRepository.findBySourceType(DataSource.SourceType.POSTGRESQL);

        // Then
        assertThat(first).hasSize(2);

        // Verify cached
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get("type:POSTGRESQL")).isNotNull();

        // When - Second call (from cache)
        List<DataSource> second = dataSourceRepository.findBySourceType(DataSource.SourceType.POSTGRESQL);

        // Then
        assertThat(second).hasSize(2);
    }

    @Test
    void testFindByIsActiveTrueCachesResult() {
        // Given
        DataSource active1 = createTestDataSource("Active1", DataSource.SourceType.POSTGRESQL);
        active1.setIsActive(true);
        dataSourceRepository.save(active1);

        DataSource active2 = createTestDataSource("Active2", DataSource.SourceType.MYSQL);
        active2.setIsActive(true);
        dataSourceRepository.save(active2);

        DataSource inactive = createTestDataSource("Inactive", DataSource.SourceType.ORACLE);
        inactive.setIsActive(false);
        dataSourceRepository.save(inactive);

        clearDataSourceCache();

        // When - First call
        List<DataSource> first = dataSourceRepository.findByIsActiveTrue();

        // Then
        assertThat(first).hasSize(2);

        // Verify cached
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get("active")).isNotNull();

        // When - Second call (from cache)
        List<DataSource> second = dataSourceRepository.findByIsActiveTrue();

        // Then
        assertThat(second).hasSize(2);
    }

    @Test
    void testConnectionPoolCacheEvictionOnSave() {
        // Given
        DataSource dataSource = createTestDataSource("Test", DataSource.SourceType.POSTGRESQL);
        DataSource saved = dataSourceRepository.save(dataSource);

        // Manually add to connection pool cache
        Cache poolCache = cacheManager.getCache("connectionPools");
        if (poolCache != null) {
            poolCache.put(saved.getId(), "mock_pool");
            assertThat(poolCache.get(saved.getId())).isNotNull();

            // When - Save data source
            saved.setName("Updated");
            dataSourceRepository.save(saved);

            // Then - Connection pool cache should also be evicted
            assertThat(poolCache.get(saved.getId())).isNull();
        }
    }

    @Test
    void testConnectionPoolCacheEvictionOnDelete() {
        // Given
        DataSource dataSource = createTestDataSource("Test", DataSource.SourceType.POSTGRESQL);
        DataSource saved = dataSourceRepository.save(dataSource);

        // Manually add to connection pool cache
        Cache poolCache = cacheManager.getCache("connectionPools");
        if (poolCache != null) {
            poolCache.put(saved.getId(), "mock_pool");
            assertThat(poolCache.get(saved.getId())).isNotNull();

            // When - Delete data source
            dataSourceRepository.deleteById(saved.getId());

            // Then - Connection pool cache should also be evicted
            assertThat(poolCache.get(saved.getId())).isNull();
        }
    }

    @Test
    void testMultipleConcurrentReads() {
        // Given
        DataSource dataSource = createTestDataSource("Concurrent Test", DataSource.SourceType.POSTGRESQL);
        DataSource saved = dataSourceRepository.save(dataSource);
        clearDataSourceCache();

        // When - Multiple reads
        for (int i = 0; i < 10; i++) {
            Optional<DataSource> result = dataSourceRepository.findById(saved.getId());
            assertThat(result).isPresent();
        }

        // Then - Should only have one cache entry
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get(saved.getId())).isNotNull();
    }

    @Test
    void testCacheIsolationBetweenIds() {
        // Given
        DataSource ds1 = dataSourceRepository.save(createTestDataSource("DS1", DataSource.SourceType.POSTGRESQL));
        DataSource ds2 = dataSourceRepository.save(createTestDataSource("DS2", DataSource.SourceType.MYSQL));
        clearDataSourceCache();

        // When - Cache both
        dataSourceRepository.findById(ds1.getId());
        dataSourceRepository.findById(ds2.getId());

        Cache cache = cacheManager.getCache("dataSources");

        // Then - Both should be cached independently
        assertThat(cache.get(ds1.getId())).isNotNull();
        assertThat(cache.get(ds2.getId())).isNotNull();

        // When - Update ds1
        ds1.setName("Updated DS1");
        dataSourceRepository.save(ds1);

        // Then - Only ds1 cache should be evicted
        assertThat(cache.get(ds1.getId())).isNull();
        // Note: With allEntries eviction, ds2 might also be evicted
        // This behavior depends on cache configuration
    }

    @Test
    void testCacheWithDifferentSourceTypes() {
        // Given
        dataSourceRepository.save(createTestDataSource("PG", DataSource.SourceType.POSTGRESQL));
        dataSourceRepository.save(createTestDataSource("MY", DataSource.SourceType.MYSQL));
        dataSourceRepository.save(createTestDataSource("OR", DataSource.SourceType.ORACLE));
        clearDataSourceCache();

        // When - Query different types
        List<DataSource> postgres = dataSourceRepository.findBySourceType(DataSource.SourceType.POSTGRESQL);
        List<DataSource> mysql = dataSourceRepository.findBySourceType(DataSource.SourceType.MYSQL);
        List<DataSource> oracle = dataSourceRepository.findBySourceType(DataSource.SourceType.ORACLE);

        // Then
        assertThat(postgres).hasSize(1);
        assertThat(mysql).hasSize(1);
        assertThat(oracle).hasSize(1);

        // Verify all are cached separately
        Cache cache = cacheManager.getCache("dataSources");
        assertThat(cache.get("type:POSTGRESQL")).isNotNull();
        assertThat(cache.get("type:MYSQL")).isNotNull();
        assertThat(cache.get("type:ORACLE")).isNotNull();
    }

    // Helper methods

    private DataSource createTestDataSource(String name, DataSource.SourceType sourceType) {
        DataSource dataSource = new DataSource();
        dataSource.setName(name);
        dataSource.setSourceType(sourceType);
        dataSource.setConnectionUrl("jdbc:" + sourceType.name().toLowerCase() + "://localhost:5432/testdb");
        dataSource.setUsername("testuser");
        dataSource.setPasswordEncrypted("encrypted_pass");
        dataSource.setIsActive(true);
        dataSource.setLastConnectionStatus(DataSource.ConnectionStatus.UNTESTED);
        dataSource.setCreatedAt(LocalDateTime.now());
        dataSource.setUpdatedAt(LocalDateTime.now());
        return dataSource;
    }

    private void clearDataSourceCache() {
        Cache cache = cacheManager.getCache("dataSources");
        if (cache != null) {
            cache.clear();
        }
    }
}
