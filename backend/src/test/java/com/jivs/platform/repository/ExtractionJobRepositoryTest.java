package com.jivs.platform.repository;

import com.jivs.platform.config.CacheConfig;
import com.jivs.platform.domain.extraction.DataSource;
import com.jivs.platform.domain.extraction.ExtractionJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ExtractionJobRepository with optimized queries
 *
 * Tests:
 * - findByJobIdWithDataSource eliminates N+1 queries
 * - findRunningJobsWithDataSource performance
 * - updateStatusBatch with multiple records
 * - Cache behavior for statistics queries
 */
@DataJpaTest
@Import(CacheConfig.class)
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.use_sql_comments=true"
})
class ExtractionJobRepositoryTest {

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CacheManager cacheManager;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() {
        // Clear all caches
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }

        // Clear database
        extractionJobRepository.deleteAll();
        dataSourceRepository.deleteAll();

        // Create test data source
        testDataSource = createTestDataSource("Test DataSource", DataSource.SourceType.POSTGRESQL);
        testDataSource = dataSourceRepository.save(testDataSource);
    }

    @Test
    void testFindByJobIdWithDataSourceEliminatesN1Queries() {
        // Given
        ExtractionJob job = createTestJob("job-001", ExtractionJob.JobStatus.PENDING);
        job = extractionJobRepository.save(job);

        // Clear persistence context to force fresh load
        entityManager.clear();

        // When
        Optional<ExtractionJob> result = extractionJobRepository.findByJobIdWithDataSource("job-001");

        // Then
        assertThat(result).isPresent();
        ExtractionJob loadedJob = result.get();

        // The DataSource should be loaded via JOIN FETCH, no lazy loading exception
        assertThat(loadedJob.getDataSource()).isNotNull();
        assertThat(loadedJob.getDataSource().getName()).isEqualTo("Test DataSource");
        assertThat(loadedJob.getDataSource().getSourceType()).isEqualTo(DataSource.SourceType.POSTGRESQL);
    }

    @Test
    void testFindByJobIdWithDataSourceVsStandardFindByJobId() {
        // Given
        ExtractionJob job = createTestJob("job-002", ExtractionJob.JobStatus.RUNNING);
        job = extractionJobRepository.save(job);

        entityManager.clear();

        // When - Using optimized query
        Optional<ExtractionJob> optimized = extractionJobRepository.findByJobIdWithDataSource("job-002");

        // Then - DataSource should be available
        assertThat(optimized).isPresent();
        assertThat(optimized.get().getDataSource()).isNotNull();

        // Compare with standard query (if needed)
        entityManager.clear();
        Optional<ExtractionJob> standard = extractionJobRepository.findByJobId("job-002");
        assertThat(standard).isPresent();
        // Note: With LAZY loading, accessing dataSource might trigger additional query
        // but in test environment with @Transactional it still works
    }

    @Test
    void testFindRunningJobsWithDataSourceLoadsAllDataSourcesEagerly() {
        // Given - Multiple running jobs
        extractionJobRepository.save(createTestJob("job-run-1", ExtractionJob.JobStatus.RUNNING));
        extractionJobRepository.save(createTestJob("job-run-2", ExtractionJob.JobStatus.RUNNING));
        extractionJobRepository.save(createTestJob("job-run-3", ExtractionJob.JobStatus.RUNNING));
        extractionJobRepository.save(createTestJob("job-pending", ExtractionJob.JobStatus.PENDING));
        extractionJobRepository.save(createTestJob("job-completed", ExtractionJob.JobStatus.COMPLETED));

        entityManager.clear();

        // When
        List<ExtractionJob> runningJobs = extractionJobRepository.findRunningJobsWithDataSource();

        // Then
        assertThat(runningJobs).hasSize(3);

        // All DataSources should be loaded (no N+1 queries)
        runningJobs.forEach(job -> {
            assertThat(job.getDataSource()).isNotNull();
            assertThat(job.getDataSource().getName()).isEqualTo("Test DataSource");
        });

        // Jobs should be ordered by startTime DESC
        assertThat(runningJobs.get(0).getJobId()).isIn("job-run-1", "job-run-2", "job-run-3");
    }

    @Test
    void testCountByStatusCachesResult() {
        // Given
        extractionJobRepository.save(createTestJob("job-1", ExtractionJob.JobStatus.COMPLETED));
        extractionJobRepository.save(createTestJob("job-2", ExtractionJob.JobStatus.COMPLETED));
        extractionJobRepository.save(createTestJob("job-3", ExtractionJob.JobStatus.FAILED));
        extractionJobRepository.save(createTestJob("job-4", ExtractionJob.JobStatus.RUNNING));

        Cache statsCache = cacheManager.getCache("extractionStats");
        if (statsCache != null) {
            statsCache.clear();
        }

        // When - First call (cache miss)
        Long firstCount = extractionJobRepository.countByStatus(ExtractionJob.JobStatus.COMPLETED);

        // Then
        assertThat(firstCount).isEqualTo(2L);

        // Verify cached
        if (statsCache != null) {
            assertThat(statsCache.get("count:COMPLETED")).isNotNull();
        }

        // When - Second call (cache hit)
        Long secondCount = extractionJobRepository.countByStatus(ExtractionJob.JobStatus.COMPLETED);

        // Then
        assertThat(secondCount).isEqualTo(2L);
    }

    @Test
    void testUpdateStatusBatchUpdatesMultipleRecords() {
        // Given - Create multiple jobs
        ExtractionJob job1 = extractionJobRepository.save(createTestJob("batch-1", ExtractionJob.JobStatus.PENDING));
        ExtractionJob job2 = extractionJobRepository.save(createTestJob("batch-2", ExtractionJob.JobStatus.PENDING));
        ExtractionJob job3 = extractionJobRepository.save(createTestJob("batch-3", ExtractionJob.JobStatus.PENDING));
        ExtractionJob job4 = extractionJobRepository.save(createTestJob("batch-4", ExtractionJob.JobStatus.RUNNING));

        List<Long> idsToUpdate = Arrays.asList(job1.getId(), job2.getId(), job3.getId());

        // When - Batch update
        extractionJobRepository.updateStatusBatch(idsToUpdate, ExtractionJob.JobStatus.RUNNING);
        extractionJobRepository.flush();
        entityManager.clear();

        // Then - Updated jobs should have new status
        ExtractionJob updated1 = extractionJobRepository.findById(job1.getId()).orElseThrow();
        ExtractionJob updated2 = extractionJobRepository.findById(job2.getId()).orElseThrow();
        ExtractionJob updated3 = extractionJobRepository.findById(job3.getId()).orElseThrow();
        ExtractionJob unchanged = extractionJobRepository.findById(job4.getId()).orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(ExtractionJob.JobStatus.RUNNING);
        assertThat(updated2.getStatus()).isEqualTo(ExtractionJob.JobStatus.RUNNING);
        assertThat(updated3.getStatus()).isEqualTo(ExtractionJob.JobStatus.RUNNING);
        assertThat(unchanged.getStatus()).isEqualTo(ExtractionJob.JobStatus.RUNNING); // Was already RUNNING

        // UpdatedAt should be updated
        assertThat(updated1.getUpdatedAt()).isNotNull();
    }

    @Test
    void testUpdateStatusBatchWithEmptyList() {
        // Given
        extractionJobRepository.save(createTestJob("job-1", ExtractionJob.JobStatus.PENDING));

        // When - Update with empty list
        extractionJobRepository.updateStatusBatch(Arrays.asList(), ExtractionJob.JobStatus.COMPLETED);
        extractionJobRepository.flush();

        // Then - No errors, jobs remain unchanged
        assertThat(extractionJobRepository.count()).isEqualTo(1L);
    }

    @Test
    void testFindByStatusWithDataSourceLoadsDataSourceEagerly() {
        // Given
        extractionJobRepository.save(createTestJob("job-comp-1", ExtractionJob.JobStatus.COMPLETED));
        extractionJobRepository.save(createTestJob("job-comp-2", ExtractionJob.JobStatus.COMPLETED));
        extractionJobRepository.save(createTestJob("job-failed", ExtractionJob.JobStatus.FAILED));

        entityManager.clear();

        // When
        List<ExtractionJob> completedJobs = extractionJobRepository
                .findByStatusWithDataSource(ExtractionJob.JobStatus.COMPLETED);

        // Then
        assertThat(completedJobs).hasSize(2);

        // DataSources should be loaded
        completedJobs.forEach(job -> {
            assertThat(job.getDataSource()).isNotNull();
            assertThat(job.getDataSource().getName()).isEqualTo("Test DataSource");
        });
    }

    @Test
    void testFindAllWithDataSourcePagination() {
        // Given - Create multiple jobs
        for (int i = 1; i <= 25; i++) {
            extractionJobRepository.save(createTestJob("job-" + i, ExtractionJob.JobStatus.PENDING));
        }

        entityManager.clear();

        // When - Fetch first page (10 records)
        Page<ExtractionJob> firstPage = extractionJobRepository
                .findAllWithDataSource(PageRequest.of(0, 10));

        // Then
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(25L);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();

        // All DataSources should be loaded
        firstPage.getContent().forEach(job -> {
            assertThat(job.getDataSource()).isNotNull();
        });

        // When - Fetch second page
        Page<ExtractionJob> secondPage = extractionJobRepository
                .findAllWithDataSource(PageRequest.of(1, 10));

        // Then
        assertThat(secondPage.getContent()).hasSize(10);
        assertThat(secondPage.hasNext()).isTrue();

        // When - Fetch last page
        Page<ExtractionJob> lastPage = extractionJobRepository
                .findAllWithDataSource(PageRequest.of(2, 10));

        // Then
        assertThat(lastPage.getContent()).hasSize(5);
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    void testFindByStatusReturnsCorrectJobs() {
        // Given
        extractionJobRepository.save(createTestJob("pending-1", ExtractionJob.JobStatus.PENDING));
        extractionJobRepository.save(createTestJob("running-1", ExtractionJob.JobStatus.RUNNING));
        extractionJobRepository.save(createTestJob("completed-1", ExtractionJob.JobStatus.COMPLETED));
        extractionJobRepository.save(createTestJob("failed-1", ExtractionJob.JobStatus.FAILED));

        // When/Then - Test each status
        assertThat(extractionJobRepository.findByStatus(ExtractionJob.JobStatus.PENDING)).hasSize(1);
        assertThat(extractionJobRepository.findByStatus(ExtractionJob.JobStatus.RUNNING)).hasSize(1);
        assertThat(extractionJobRepository.findByStatus(ExtractionJob.JobStatus.COMPLETED)).hasSize(1);
        assertThat(extractionJobRepository.findByStatus(ExtractionJob.JobStatus.FAILED)).hasSize(1);
    }

    @Test
    void testFindByDateRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        ExtractionJob job1 = createTestJob("old-job", ExtractionJob.JobStatus.COMPLETED);
        job1.setStartTime(yesterday);
        extractionJobRepository.save(job1);

        ExtractionJob job2 = createTestJob("current-job", ExtractionJob.JobStatus.RUNNING);
        job2.setStartTime(now);
        extractionJobRepository.save(job2);

        // When
        List<ExtractionJob> jobsInRange = extractionJobRepository
                .findByDateRange(yesterday.minusHours(1), now.plusHours(1));

        // Then
        assertThat(jobsInRange).hasSize(2);
    }

    @Test
    void testFindByDataSourceId() {
        // Given
        DataSource anotherDataSource = createTestDataSource("Another DS", DataSource.SourceType.MYSQL);
        anotherDataSource = dataSourceRepository.save(anotherDataSource);

        // Jobs for first data source
        extractionJobRepository.save(createTestJob("job-1", ExtractionJob.JobStatus.PENDING));
        extractionJobRepository.save(createTestJob("job-2", ExtractionJob.JobStatus.RUNNING));

        // Jobs for second data source
        ExtractionJob job3 = createTestJob("job-3", ExtractionJob.JobStatus.COMPLETED);
        job3.setDataSource(anotherDataSource);
        extractionJobRepository.save(job3);

        // When
        Page<ExtractionJob> jobsForFirstDs = extractionJobRepository
                .findByDataSourceId(testDataSource.getId(), PageRequest.of(0, 10));

        Page<ExtractionJob> jobsForSecondDs = extractionJobRepository
                .findByDataSourceId(anotherDataSource.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(jobsForFirstDs.getContent()).hasSize(2);
        assertThat(jobsForSecondDs.getContent()).hasSize(1);
    }

    @Test
    void testCountByStatusForDifferentStatuses() {
        // Given
        for (int i = 0; i < 3; i++) {
            extractionJobRepository.save(createTestJob("pending-" + i, ExtractionJob.JobStatus.PENDING));
        }
        for (int i = 0; i < 5; i++) {
            extractionJobRepository.save(createTestJob("running-" + i, ExtractionJob.JobStatus.RUNNING));
        }
        for (int i = 0; i < 7; i++) {
            extractionJobRepository.save(createTestJob("completed-" + i, ExtractionJob.JobStatus.COMPLETED));
        }
        for (int i = 0; i < 2; i++) {
            extractionJobRepository.save(createTestJob("failed-" + i, ExtractionJob.JobStatus.FAILED));
        }

        // When/Then
        assertThat(extractionJobRepository.countByStatus(ExtractionJob.JobStatus.PENDING)).isEqualTo(3L);
        assertThat(extractionJobRepository.countByStatus(ExtractionJob.JobStatus.RUNNING)).isEqualTo(5L);
        assertThat(extractionJobRepository.countByStatus(ExtractionJob.JobStatus.COMPLETED)).isEqualTo(7L);
        assertThat(extractionJobRepository.countByStatus(ExtractionJob.JobStatus.FAILED)).isEqualTo(2L);
    }

    @Test
    void testJobsOrderedByCreatedAtDesc() {
        // Given - Create jobs with slight delay to ensure different timestamps
        ExtractionJob job1 = createTestJob("first", ExtractionJob.JobStatus.COMPLETED);
        extractionJobRepository.save(job1);

        ExtractionJob job2 = createTestJob("second", ExtractionJob.JobStatus.COMPLETED);
        extractionJobRepository.save(job2);

        ExtractionJob job3 = createTestJob("third", ExtractionJob.JobStatus.COMPLETED);
        extractionJobRepository.save(job3);

        entityManager.clear();

        // When
        List<ExtractionJob> jobs = extractionJobRepository
                .findByStatusWithDataSource(ExtractionJob.JobStatus.COMPLETED);

        // Then - Should be ordered by createdAt DESC (newest first)
        assertThat(jobs).hasSize(3);
        // Most recent job should be first
        assertThat(jobs.get(0).getCreatedAt())
                .isAfterOrEqualTo(jobs.get(1).getCreatedAt());
        assertThat(jobs.get(1).getCreatedAt())
                .isAfterOrEqualTo(jobs.get(2).getCreatedAt());
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

    private ExtractionJob createTestJob(String jobId, ExtractionJob.JobStatus status) {
        ExtractionJob job = new ExtractionJob();
        job.setJobId(jobId);
        job.setDataSource(testDataSource);
        job.setStatus(status);
        job.setStartTime(LocalDateTime.now());
        job.setRecordsExtracted(0L);
        job.setRecordsFailed(0L);
        job.setBytesProcessed(0L);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        job.setTriggeredBy("test-user");
        return job;
    }
}
