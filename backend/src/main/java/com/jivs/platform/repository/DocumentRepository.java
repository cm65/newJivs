package com.jivs.platform.repository;

import com.jivs.platform.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository for Document entities
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find documents by status
     */
    Page<Document> findByStatus(String status, Pageable pageable);

    /**
     * Find documents by archived status
     */
    Page<Document> findByArchived(boolean archived, Pageable pageable);

    /**
     * Find documents by status and archived
     */
    Page<Document> findByStatusAndArchived(String status, boolean archived, Pageable pageable);

    /**
     * Find documents by file type
     */
    List<Document> findByFileType(String fileType);

    /**
     * Find documents by author
     */
    List<Document> findByAuthor(String author);

    /**
     * Find documents created between dates
     */
    List<Document> findByCreatedDateBetween(Date startDate, Date endDate);

    /**
     * Find documents by title containing text (case-insensitive)
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<Document> findByTitleContainingIgnoreCase(@Param("searchText") String searchText);

    /**
     * Find documents by content containing text (case-insensitive)
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.content) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<Document> findByContentContainingIgnoreCase(@Param("searchText") String searchText);

    /**
     * Find documents by tag
     */
    @Query("SELECT d FROM Document d JOIN d.tags t WHERE t = :tag")
    List<Document> findByTag(@Param("tag") String tag);

    /**
     * Count documents by status
     */
    Long countByStatus(String status);

    /**
     * Count documents by archived status
     */
    Long countByArchived(boolean archived);

    /**
     * Count documents by file type
     */
    Long countByFileType(String fileType);

    /**
     * Sum size by file type
     */
    @Query("SELECT COALESCE(SUM(d.size), 0) FROM Document d WHERE d.fileType = :fileType")
    Long sumSizeByFileType(@Param("fileType") String fileType);

    /**
     * Get total storage used
     */
    @Query("SELECT COALESCE(SUM(d.size), 0) FROM Document d")
    Long getTotalStorageUsed();

    /**
     * Find documents needing archival (older than specified days)
     */
    @Query("SELECT d FROM Document d WHERE d.archived = false AND d.createdDate < :date")
    List<Document> findDocumentsForArchival(@Param("date") Date date);

    /**
     * Find documents by storage tier
     */
    List<Document> findByStorageTier(String storageTier);

    /**
     * Find documents past retention date
     */
    @Query("SELECT d FROM Document d WHERE d.retentionDate IS NOT NULL AND d.retentionDate < CURRENT_DATE")
    List<Document> findDocumentsPastRetention();

    /**
     * Search documents by multiple fields
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN d.tags t WHERE " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("query") String query, Pageable pageable);

    /**
     * Get documents by archive ID
     */
    Document findByArchiveId(String archiveId);

    /**
     * Find documents with no content extracted
     */
    @Query("SELECT d FROM Document d WHERE d.content IS NULL OR d.content = ''")
    List<Document> findDocumentsWithoutContent();

    /**
     * Get document statistics by date range
     */
    @Query("SELECT DATE(d.createdDate) as date, COUNT(d) as count, SUM(d.size) as totalSize " +
           "FROM Document d WHERE d.createdDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(d.createdDate)")
    List<Object[]> getDocumentStatisticsByDateRange(@Param("startDate") Date startDate,
                                                     @Param("endDate") Date endDate);
}