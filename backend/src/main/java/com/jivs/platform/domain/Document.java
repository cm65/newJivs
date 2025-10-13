package com.jivs.platform.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Document entity for storing document metadata
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Column(name = "file_type", length = 10)
    private String fileType;

    @Column(name = "size")
    private Long size;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "archive_id")
    private String archiveId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "archived")
    private boolean archived;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "author")
    private String author;

    @Column(name = "subject")
    private String subject;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "language", length = 10)
    private String language;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_date")
    private Date modifiedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "accessed_date")
    private Date accessedDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "retention_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date retentionDate;

    @Column(name = "encrypted")
    private boolean encrypted;

    @Column(name = "compressed")
    private boolean compressed;

    @Column(name = "compression_ratio")
    private Double compressionRatio;

    @Column(name = "storage_tier")
    private String storageTier;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Date();
        }
        if (modifiedDate == null) {
            modifiedDate = new Date();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
    }

    // Explicit getters and setters to work around Lombok compilation issues
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public Date getAccessedDate() { return accessedDate; }
    public void setAccessedDate(Date accessedDate) { this.accessedDate = accessedDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public Date getRetentionDate() { return retentionDate; }
    public void setRetentionDate(Date retentionDate) { this.retentionDate = retentionDate; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public boolean isCompressed() { return compressed; }
    public void setCompressed(boolean compressed) { this.compressed = compressed; }

    public Double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }

    public String getStorageTier() { return storageTier; }
    public void setStorageTier(String storageTier) { this.storageTier = storageTier; }
}