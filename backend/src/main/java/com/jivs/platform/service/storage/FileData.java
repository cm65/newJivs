package com.jivs.platform.service.storage;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * File data with metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileData {
    private String storageId;
    private String filename;
    private byte[] data;
    private String contentType;
    private long size;
    private Map<String, String> metadata;

    // Explicit getters and setters to work around Lombok compilation issues
    public String getStorageId() { return storageId; }
    public void setStorageId(String storageId) { this.storageId = storageId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}