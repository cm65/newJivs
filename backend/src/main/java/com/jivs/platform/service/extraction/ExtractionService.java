package com.jivs.platform.service.extraction;

import com.jivs.platform.common.constant.Constants;
import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.exception.ResourceNotFoundException;
import com.jivs.platform.common.util.StringUtil;
import com.jivs.platform.domain.extraction.DataSource;
import com.jivs.platform.domain.extraction.ExtractionJob;
import com.jivs.platform.repository.DataSourceRepository;
import com.jivs.platform.repository.ExtractionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for data extraction operations
 */
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractionService.class);

    private final ExtractionJobRepository extractionJobRepository;
    private final DataSourceRepository dataSourceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ConnectorFactory connectorFactory;

    /**
     * Create a new extraction job
     */
    @Transactional
    public ExtractionJob createExtractionJob(Long dataSourceId, Map<String, String> parameters, String triggeredBy) {
        log.info("Creating extraction job for data source: {}", dataSourceId);

        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", dataSourceId));

        if (!dataSource.getIsActive()) {
            throw new BusinessException("Data source is not active: " + dataSource.getName());
        }

        // P0.4: Check for running jobs with optimized query (avoids N+1)
        List<ExtractionJob> runningJobs = extractionJobRepository.findRunningJobsWithDataSource();
        if (runningJobs.stream().anyMatch(job -> job.getDataSource().getId().equals(dataSourceId))) {
            throw new BusinessException("An extraction job is already running for this data source");
        }

        ExtractionJob job = new ExtractionJob();
        job.setJobId(StringUtil.generateUUID());
        job.setDataSource(dataSource);
        job.setStatus(ExtractionJob.JobStatus.PENDING);
        job.setExtractionParams(parameters != null ? parameters : new HashMap<>());
        job.setTriggeredBy(triggeredBy);

        ExtractionJob savedJob = extractionJobRepository.save(job);
        log.info("Extraction job created: {}", savedJob.getJobId());

        // Queue job for processing
        queueExtractionJob(savedJob);

        return savedJob;
    }

    /**
     * Queue extraction job for async processing
     */
    private void queueExtractionJob(ExtractionJob job) {
        Map<String, Object> message = new HashMap<>();
        message.put("jobId", job.getJobId());
        message.put("dataSourceId", job.getDataSource().getId());
        message.put("parameters", job.getExtractionParams());

        rabbitTemplate.convertAndSend("jivs.exchange", "extraction.start", message);
        log.info("Extraction job queued: {}", job.getJobId());
    }

    /**
     * Execute extraction job asynchronously
     */
    @Async
    @Transactional
    public CompletableFuture<ExtractionJob> executeExtractionJob(String jobId) {
        log.info("Starting extraction job execution: {}", jobId);

        // P0.4: Use optimized query with JOIN FETCH to eliminate N+1 query
        ExtractionJob job = extractionJobRepository.findByJobIdWithDataSource(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionJob", "jobId", jobId));

        try {
            // Update status to running
            job.setStatus(ExtractionJob.JobStatus.RUNNING);
            job.setStartTime(LocalDateTime.now());
            extractionJobRepository.save(job);

            // Get appropriate connector
            DataConnector connector = connectorFactory.getConnector(job.getDataSource());

            // Test connection
            if (!connector.testConnection()) {
                throw new BusinessException("Failed to connect to data source");
            }

            // Execute extraction
            ExtractionResult result = connector.extract(job.getExtractionParams());

            // Update job with results
            job.setStatus(ExtractionJob.JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            job.setRecordsExtracted(result.getRecordsExtracted());
            job.setRecordsFailed(result.getRecordsFailed());
            job.setBytesProcessed(result.getBytesProcessed());

            log.info("Extraction job completed successfully: {} - Records: {}",
                    jobId, result.getRecordsExtracted());

        } catch (Exception e) {
            log.error("Extraction job failed: {}", jobId, e);
            job.setStatus(ExtractionJob.JobStatus.FAILED);
            job.setEndTime(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
            job.setErrorStackTrace(getStackTraceString(e));
        }

        ExtractionJob updatedJob = extractionJobRepository.save(job);

        // Send completion notification
        sendCompletionNotification(updatedJob);

        return CompletableFuture.completedFuture(updatedJob);
    }

    /**
     * Get extraction job by ID
     */
    public ExtractionJob getExtractionJob(String jobId) {
        return extractionJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionJob", "jobId", jobId));
    }

    /**
     * Get all extraction jobs with pagination
     */
    public Page<ExtractionJob> getAllExtractionJobs(Pageable pageable) {
        return extractionJobRepository.findAll(pageable);
    }

    /**
     * Get extraction jobs by data source
     */
    public Page<ExtractionJob> getExtractionJobsByDataSource(Long dataSourceId, Pageable pageable) {
        return extractionJobRepository.findByDataSourceId(dataSourceId, pageable);
    }

    /**
     * Get extraction jobs by status
     */
    public List<ExtractionJob> getExtractionJobsByStatus(ExtractionJob.JobStatus status) {
        return extractionJobRepository.findByStatus(status);
    }

    /**
     * Cancel extraction job
     */
    @Transactional
    public ExtractionJob cancelExtractionJob(String jobId) {
        log.info("Cancelling extraction job: {}", jobId);

        ExtractionJob job = getExtractionJob(jobId);

        if (job.getStatus() != ExtractionJob.JobStatus.PENDING &&
            job.getStatus() != ExtractionJob.JobStatus.RUNNING) {
            throw new BusinessException("Cannot cancel job in status: " + job.getStatus());
        }

        job.setStatus(ExtractionJob.JobStatus.CANCELLED);
        job.setEndTime(LocalDateTime.now());

        ExtractionJob updatedJob = extractionJobRepository.save(job);
        log.info("Extraction job cancelled: {}", jobId);

        return updatedJob;
    }

    /**
     * Retry failed extraction job
     */
    @Transactional
    public ExtractionJob retryExtractionJob(String jobId, String triggeredBy) {
        log.info("Retrying extraction job: {}", jobId);

        ExtractionJob originalJob = getExtractionJob(jobId);

        if (originalJob.getStatus() != ExtractionJob.JobStatus.FAILED) {
            throw new BusinessException("Can only retry failed jobs");
        }

        // Create new job with same parameters
        return createExtractionJob(
                originalJob.getDataSource().getId(),
                originalJob.getExtractionParams(),
                triggeredBy
        );
    }

    /**
     * Get extraction job statistics
     */
    public Map<String, Object> getExtractionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalJobs", extractionJobRepository.count());
        stats.put("pendingJobs", extractionJobRepository.countByStatus(ExtractionJob.JobStatus.PENDING));
        stats.put("runningJobs", extractionJobRepository.countByStatus(ExtractionJob.JobStatus.RUNNING));
        stats.put("completedJobs", extractionJobRepository.countByStatus(ExtractionJob.JobStatus.COMPLETED));
        stats.put("failedJobs", extractionJobRepository.countByStatus(ExtractionJob.JobStatus.FAILED));

        return stats;
    }

    /**
     * Send completion notification
     */
    private void sendCompletionNotification(ExtractionJob job) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("jobId", job.getJobId());
        notification.put("status", job.getStatus());
        notification.put("dataSource", job.getDataSource().getName());
        notification.put("recordsExtracted", job.getRecordsExtracted());

        rabbitTemplate.convertAndSend("jivs.exchange", "notification.extraction", notification);
    }

    /**
     * Convert exception to stack trace string
     */
    private String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\t").append(element.toString()).append("\n");
            if (sb.length() > 5000) break; // Limit stack trace size
        }
        return sb.toString();
    }
}