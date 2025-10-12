package com.jivs.platform.service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for managing file storage across multiple storage backends
 * Supports local filesystem, S3, Azure Blob, and Google Cloud Storage
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageService.class);

    @Value("${storage.base-path:/data/storage}")
    private String basePath;

    @Value("${storage.temp-path:/data/temp}")
    private String tempPath;

    @Value("${storage.max-file-size:104857600}") // 100MB default
    private long maxFileSize;

    private final EncryptionService encryptionService;

    /**
     * Store a file
     */
    public StorageResult storeFile(MultipartFile file, StorageOptions options) throws IOException {
        log.info("Storing file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

        // Validate file
        validateFile(file);

        StorageResult result = new StorageResult();
        result.setOriginalFilename(file.getOriginalFilename());
        result.setSize(file.getSize());
        result.setContentType(file.getContentType());
        result.setStorageId(UUID.randomUUID().toString());
        result.setUploadTimestamp(new Date());

        try {
            // Determine storage location
            StorageLocation location = determineStorageLocation(options);
            result.setLocation(location);

            // Get file data
            byte[] fileData = file.getBytes();

            // Encrypt if required
            if (options.isEncrypted()) {
                fileData = encryptionService.encrypt(fileData);
                result.setEncrypted(true);
            }

            // Store based on location
            String storedPath = storeToLocation(location, result.getStorageId(), fileData, options);
            result.setPath(storedPath);

            // Generate checksum
            String checksum = generateChecksum(fileData);
            result.setChecksum(checksum);

            // Store metadata
            storeMetadata(result, options);

            log.info("File stored successfully: {} at {}", result.getStorageId(), storedPath);

        } catch (Exception e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new IOException("Failed to store file", e);
        }

        return result;
    }

    /**
     * Store file from byte array
     */
    public StorageResult storeFile(String filename, byte[] data, StorageOptions options) throws IOException {
        log.info("Storing file from bytes: {} (size: {} bytes)", filename, data.length);

        StorageResult result = new StorageResult();
        result.setOriginalFilename(filename);
        result.setSize(data.length);
        result.setStorageId(UUID.randomUUID().toString());
        result.setUploadTimestamp(new Date());

        try {
            StorageLocation location = determineStorageLocation(options);
            result.setLocation(location);

            byte[] fileData = data;

            if (options.isEncrypted()) {
                fileData = encryptionService.encrypt(fileData);
                result.setEncrypted(true);
            }

            String storedPath = storeToLocation(location, result.getStorageId(), fileData, options);
            result.setPath(storedPath);

            String checksum = generateChecksum(fileData);
            result.setChecksum(checksum);

            storeMetadata(result, options);

            log.info("File stored successfully: {}", result.getStorageId());

        } catch (Exception e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new IOException("Failed to store file", e);
        }

        return result;
    }

    /**
     * Retrieve a file
     */
    public FileData retrieveFile(String storageId) throws IOException {
        log.info("Retrieving file: {}", storageId);

        try {
            // Get metadata
            StorageMetadata metadata = getMetadata(storageId);
            if (metadata == null) {
                throw new FileNotFoundException("File not found: " + storageId);
            }

            // Retrieve file data
            byte[] data = retrieveFromLocation(metadata.getLocation(), metadata.getPath());

            // Decrypt if needed
            if (metadata.isEncrypted()) {
                data = encryptionService.decrypt(data);
            }

            // Verify checksum
            String checksum = generateChecksum(data);
            if (!checksum.equals(metadata.getChecksum())) {
                log.warn("Checksum mismatch for file: {}", storageId);
            }

            FileData fileData = new FileData();
            fileData.setStorageId(storageId);
            fileData.setFilename(metadata.getOriginalFilename());
            fileData.setData(data);
            fileData.setContentType(metadata.getContentType());
            fileData.setSize(data.length);

            log.info("File retrieved successfully: {}", storageId);
            return fileData;

        } catch (Exception e) {
            log.error("Failed to retrieve file: {}", e.getMessage(), e);
            throw new IOException("Failed to retrieve file", e);
        }
    }

    /**
     * Delete a file
     */
    public void deleteFile(String storageId) throws IOException {
        log.info("Deleting file: {}", storageId);

        try {
            StorageMetadata metadata = getMetadata(storageId);
            if (metadata == null) {
                log.warn("File not found for deletion: {}", storageId);
                return;
            }

            deleteFromLocation(metadata.getLocation(), metadata.getPath());
            deleteMetadata(storageId);

            log.info("File deleted successfully: {}", storageId);

        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage(), e);
            throw new IOException("Failed to delete file", e);
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String storageId) {
        try {
            StorageMetadata metadata = getMetadata(storageId);
            return metadata != null;
        } catch (Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get file metadata
     */
    public StorageMetadata getFileMetadata(String storageId) {
        return getMetadata(storageId);
    }

    /**
     * List files in a directory
     */
    public List<StorageMetadata> listFiles(String directory, StorageLocation location) throws IOException {
        log.debug("Listing files in directory: {} at {}", directory, location);

        try {
            switch (location) {
                case LOCAL:
                    return listLocalFiles(directory);
                case S3:
                    return listS3Files(directory);
                case AZURE_BLOB:
                    return listAzureFiles(directory);
                case GCS:
                    return listGcsFiles(directory);
                default:
                    throw new IllegalArgumentException("Unsupported storage location: " + location);
            }
        } catch (Exception e) {
            log.error("Failed to list files: {}", e.getMessage(), e);
            throw new IOException("Failed to list files", e);
        }
    }

    /**
     * Move file to different storage location
     */
    public void moveFile(String storageId, StorageLocation newLocation) throws IOException {
        log.info("Moving file {} to {}", storageId, newLocation);

        try {
            // Retrieve file
            FileData fileData = retrieveFile(storageId);

            // Store in new location
            StorageOptions options = new StorageOptions();
            options.setLocation(newLocation);
            StorageResult newResult = storeFile(fileData.getFilename(), fileData.getData(), options);

            // Delete from old location
            deleteFile(storageId);

            log.info("File moved successfully: {} -> {}", storageId, newResult.getStorageId());

        } catch (Exception e) {
            log.error("Failed to move file: {}", e.getMessage(), e);
            throw new IOException("Failed to move file", e);
        }
    }

    /**
     * Copy file
     */
    public StorageResult copyFile(String storageId, StorageOptions options) throws IOException {
        log.info("Copying file: {}", storageId);

        try {
            FileData fileData = retrieveFile(storageId);
            return storeFile(fileData.getFilename(), fileData.getData(), options);
        } catch (Exception e) {
            log.error("Failed to copy file: {}", e.getMessage(), e);
            throw new IOException("Failed to copy file", e);
        }
    }

    /**
     * Store data export file (convenience method for compliance service)
     */
    public String storeDataExport(Map<String, Object> data, String format) {
        log.info("Storing data export in format: {}", format);

        try {
            // Convert data to bytes based on format
            byte[] exportData = convertDataToFormat(data, format);

            // Create filename
            String filename = "data_export_" + System.currentTimeMillis() + "." + format.toLowerCase();

            // Create storage options
            StorageOptions options = new StorageOptions();
            options.setDirectory("compliance/exports");
            options.setEncrypted(true);  // Always encrypt compliance exports

            // Store the file
            StorageResult result = storeFile(filename, exportData, options);

            log.info("Data export stored successfully: {}", result.getPath());
            return result.getPath();

        } catch (Exception e) {
            log.error("Failed to store data export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store data export", e);
        }
    }

    /**
     * Convert data to specified format
     */
    private byte[] convertDataToFormat(Map<String, Object> data, String format) {
        try {
            if (format == null) {
                format = "JSON";
            }

            switch (format.toUpperCase()) {
                case "JSON":
                    return convertToJson(data);
                case "CSV":
                    return convertToCsv(data);
                case "XML":
                    return convertToXml(data);
                default:
                    return convertToJson(data);  // Default to JSON
            }
        } catch (Exception e) {
            log.error("Failed to convert data to format: {}", format, e);
            throw new RuntimeException("Failed to convert data", e);
        }
    }

    /**
     * Convert data to JSON
     */
    private byte[] convertToJson(Map<String, Object> data) {
        // Simple JSON conversion (in production, use Jackson or Gson)
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("  \"").append(entry.getKey()).append("\": ");
            json.append("\"").append(String.valueOf(entry.getValue())).append("\"");
            first = false;
        }
        json.append("\n}");
        return json.toString().getBytes();
    }

    /**
     * Convert data to CSV
     */
    private byte[] convertToCsv(Map<String, Object> data) {
        // Simple CSV conversion
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Field,Value\n");

        // Data rows
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            csv.append(escapeCsv(entry.getKey())).append(",");
            csv.append(escapeCsv(String.valueOf(entry.getValue()))).append("\n");
        }

        return csv.toString().getBytes();
    }

    /**
     * Convert data to XML
     */
    private byte[] convertToXml(Map<String, Object> data) {
        // Simple XML conversion
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<data>\n");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            xml.append("  <").append(sanitizeXmlTag(entry.getKey())).append(">");
            xml.append(escapeXml(String.valueOf(entry.getValue())));
            xml.append("</").append(sanitizeXmlTag(entry.getKey())).append(">\n");
        }

        xml.append("</data>");
        return xml.toString().getBytes();
    }

    /**
     * Escape CSV value
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Escape XML value
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    /**
     * Sanitize XML tag name
     */
    private String sanitizeXmlTag(String tag) {
        if (tag == null) {
            return "field";
        }
        return tag.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * Validate file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IOException("File size exceeds maximum allowed size");
        }

        // Additional validation as needed
    }

    /**
     * Determine storage location based on options
     */
    private StorageLocation determineStorageLocation(StorageOptions options) {
        if (options.getLocation() != null) {
            return options.getLocation();
        }

        // Default to local storage
        return StorageLocation.LOCAL;
    }

    /**
     * Store to specific location
     */
    private String storeToLocation(
            StorageLocation location,
            String storageId,
            byte[] data,
            StorageOptions options) throws IOException {

        switch (location) {
            case LOCAL:
                return storeToLocal(storageId, data, options);
            case S3:
                return storeToS3(storageId, data, options);
            case AZURE_BLOB:
                return storeToAzure(storageId, data, options);
            case GCS:
                return storeToGcs(storageId, data, options);
            default:
                throw new IllegalArgumentException("Unsupported storage location: " + location);
        }
    }

    /**
     * Store to local filesystem
     */
    private String storeToLocal(String storageId, byte[] data, StorageOptions options) throws IOException {
        Path directory = Paths.get(basePath, options.getDirectory() != null ? options.getDirectory() : "default");
        Files.createDirectories(directory);

        Path filePath = directory.resolve(storageId);
        Files.write(filePath, data);

        return filePath.toString();
    }

    /**
     * Store to S3
     */
    private String storeToS3(String storageId, byte[] data, StorageOptions options) {
        log.debug("Storing to S3: {}", storageId);
        // TODO: Implement S3 storage using AWS SDK
        return "s3://" + options.getDirectory() + "/" + storageId;
    }

    /**
     * Store to Azure Blob Storage
     */
    private String storeToAzure(String storageId, byte[] data, StorageOptions options) {
        log.debug("Storing to Azure: {}", storageId);
        // TODO: Implement Azure storage
        return "azure://" + options.getDirectory() + "/" + storageId;
    }

    /**
     * Store to Google Cloud Storage
     */
    private String storeToGcs(String storageId, byte[] data, StorageOptions options) {
        log.debug("Storing to GCS: {}", storageId);
        // TODO: Implement GCS storage
        return "gs://" + options.getDirectory() + "/" + storageId;
    }

    /**
     * Retrieve from location
     */
    private byte[] retrieveFromLocation(StorageLocation location, String path) throws IOException {
        switch (location) {
            case LOCAL:
                return Files.readAllBytes(Paths.get(path));
            case S3:
                // TODO: Implement S3 retrieval
                return new byte[0];
            case AZURE_BLOB:
                // TODO: Implement Azure retrieval
                return new byte[0];
            case GCS:
                // TODO: Implement GCS retrieval
                return new byte[0];
            default:
                throw new IllegalArgumentException("Unsupported storage location: " + location);
        }
    }

    /**
     * Delete from location
     */
    private void deleteFromLocation(StorageLocation location, String path) throws IOException {
        switch (location) {
            case LOCAL:
                Files.deleteIfExists(Paths.get(path));
                break;
            case S3:
                // TODO: Implement S3 deletion
                break;
            case AZURE_BLOB:
                // TODO: Implement Azure deletion
                break;
            case GCS:
                // TODO: Implement GCS deletion
                break;
            default:
                throw new IllegalArgumentException("Unsupported storage location: " + location);
        }
    }

    /**
     * List local files
     */
    private List<StorageMetadata> listLocalFiles(String directory) throws IOException {
        Path dirPath = Paths.get(basePath, directory);
        if (!Files.exists(dirPath)) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths
                .filter(Files::isRegularFile)
                .map(this::pathToMetadata)
                .collect(Collectors.toList());
        }
    }

    private StorageMetadata pathToMetadata(Path path) {
        StorageMetadata metadata = new StorageMetadata();
        metadata.setPath(path.toString());
        try {
            metadata.setSize(Files.size(path));
        } catch (IOException e) {
            log.error("Error getting file size: {}", e.getMessage());
        }
        return metadata;
    }

    /**
     * List S3 files
     */
    private List<StorageMetadata> listS3Files(String directory) {
        // TODO: Implement S3 listing
        return new ArrayList<>();
    }

    /**
     * List Azure files
     */
    private List<StorageMetadata> listAzureFiles(String directory) {
        // TODO: Implement Azure listing
        return new ArrayList<>();
    }

    /**
     * List GCS files
     */
    private List<StorageMetadata> listGcsFiles(String directory) {
        // TODO: Implement GCS listing
        return new ArrayList<>();
    }

    /**
     * Store metadata
     */
    private void storeMetadata(StorageResult result, StorageOptions options) {
        // TODO: Store in database
        log.debug("Storing metadata for: {}", result.getStorageId());
    }

    /**
     * Get metadata
     */
    private StorageMetadata getMetadata(String storageId) {
        // TODO: Retrieve from database
        log.debug("Getting metadata for: {}", storageId);
        return null;
    }

    /**
     * Delete metadata
     */
    private void deleteMetadata(String storageId) {
        // TODO: Delete from database
        log.debug("Deleting metadata for: {}", storageId);
    }

    /**
     * Generate checksum
     */
    private String generateChecksum(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating checksum: {}", e.getMessage());
            return "";
        }
    }
}

/**
 * Storage result
 */
class StorageResult {
    private String storageId;
    private String originalFilename;
    private String path;
    private long size;
    private String contentType;
    private String checksum;
    private boolean encrypted;
    private StorageLocation location;
    private Date uploadTimestamp;

    // Getters and setters
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
}

/**
 * Storage options
 */
class StorageOptions {
    private StorageLocation location;
    private String directory;
    private boolean encrypted;
    private Map<String, String> metadata;

    // Getters and setters
    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }
    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}

/**
 * File data
 */
class FileData {
    private String storageId;
    private String filename;
    private byte[] data;
    private String contentType;
    private long size;

    // Getters and setters
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
}

/**
 * Storage metadata
 */
class StorageMetadata {
    private String storageId;
    private String originalFilename;
    private String path;
    private long size;
    private String contentType;
    private String checksum;
    private boolean encrypted;
    private StorageLocation location;
    private Date uploadTimestamp;

    // Getters and setters
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
}

/**
 * Storage locations
 */
enum StorageLocation {
    LOCAL,
    S3,
    AZURE_BLOB,
    GCS
}
