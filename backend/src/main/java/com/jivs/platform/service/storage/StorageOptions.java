package com.jivs.platform.service.storage;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Options for storage operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageOptions {
    private String directory;
    private boolean encrypt = false;
    private boolean encrypted = false; // for backward compatibility
    private boolean compress = false;
    private StorageLocation location = StorageLocation.LOCAL;
    private boolean versioning = false;
    private String contentType;

    // Explicit getters and setters to work around Lombok compilation issues
    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }

    public boolean isEncrypt() { return encrypt; }
    public void setEncrypt(boolean encrypt) { this.encrypt = encrypt; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public boolean isCompress() { return compress; }
    public void setCompress(boolean compress) { this.compress = compress; }

    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }

    public boolean isVersioning() { return versioning; }
    public void setVersioning(boolean versioning) { this.versioning = versioning; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}