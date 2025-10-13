package com.jivs.platform.service.storage;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * Result of a storage operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {
    private String storageId;
    private String originalFilename;
    private String path;
    private long size;
    private String contentType;
    private String checksum;
    private boolean encrypted;
    private StorageLocation location;
    private Date uploadTimestamp;

    // Backward compatibility fields
    private String id;
    private boolean success = true;
    private String error;

    // Convenience methods for backward compatibility
    public String getId() {
        return storageId != null ? storageId : id;
    }

    public void setId(String id) {
        this.id = id;
        if (this.storageId == null) {
            this.storageId = id;
        }
    }

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

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}