package com.jivs.platform.service.storage;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;
import java.util.Map;

/**
 * Metadata about stored files
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageMetadata {
    private String storageId;
    private String originalFilename;
    private String path;
    private long size;
    private String contentType;
    private String checksum;
    private boolean encrypted;
    private StorageLocation location;
    private Date uploadTimestamp;

    // Additional fields for compatibility
    private String id;
    private String originalName;
    private Date created;
    private Date modified;
    private Map<String, String> customMetadata;

    // Explicit getters and setters to work around Lombok compilation issues
    public String getStorageId() { return storageId; }
    public void setStorageId(String storageId) { this.storageId = storageId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }

    public Date getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(Date uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    public Date getModified() { return modified; }
    public void setModified(Date modified) { this.modified = modified; }

    public Map<String, String> getCustomMetadata() { return customMetadata; }
    public void setCustomMetadata(Map<String, String> customMetadata) { this.customMetadata = customMetadata; }
}