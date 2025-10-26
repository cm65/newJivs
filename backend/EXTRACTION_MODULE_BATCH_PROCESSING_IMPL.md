# Batch Processing Implementation Guide

**Module:** Data Extraction - Batch Processing
**Priority:** P0 - CRITICAL (data is currently discarded)
**Effort:** 8-12 hours

---

## 🎯 OBJECTIVE

Replace placeholder `processBatch()` methods with actual file writing logic that persists extracted data.

**Current Problem:**
```java
private void processBatch(List<Map<String, Object>> batch, String outputPath, long batchNumber) {
    // TODO: Implement actual batch processing logic
    log.trace("Processing batch {} with {} records", batchNumber, batch.size());
    // DATA IS LOST HERE!
}
```

**Impact:**
- All extractions report success but produce NO output
- Users think data is extracted but it's silently discarded
- 100% data loss in current implementation

---

## 📊 DESIGN DECISIONS

### Output Formats

Support 3 formats (priority order):

1. **Parquet** (Recommended for big data)
   - Columnar format, highly compressed
   - Native support in Spark, Athena, BigQuery
   - ~10x smaller than CSV
   - Fast for analytics queries

2. **CSV** (Universal compatibility)
   - Human-readable
   - Excel-compatible
   - Works with legacy systems

3. **JSON Lines** (Developer-friendly)
   - One JSON object per line
   - Easy to parse
   - Good for streaming

### Storage Backends

Support multiple backends:

1. **Local Filesystem** (Development)
   - `/tmp/jivs/extractions/`
   - Good for testing

2. **S3** (Production - AWS)
   - `s3://jivs-extractions/`
   - Scalable, durable

3. **Azure Blob Storage** (Production - Azure)
   - `azblob://jivs-extractions/`

4. **Google Cloud Storage** (Production - GCP)
   - `gs://jivs-extractions/`

---

## 🔧 IMPLEMENTATION

### Step 1: Add Dependencies

**File:** `backend/pom.xml`

```xml
<!-- Parquet Support -->
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-avro</artifactId>
    <version>1.13.1</version>
</dependency>

<!-- Avro for Parquet Schema -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>

<!-- Hadoop for Parquet (minimal) -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.6</version>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- CSV Support -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

<!-- S3 Support (AWS SDK v2) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.160</version>
</dependency>

<!-- JSON Support (Jackson already included in Spring Boot) -->
```

### Step 2: Create Batch Writer Interface

**File:** `backend/src/main/java/com/jivs/platform/service/extraction/batch/BatchWriter.java`

```java
package com.jivs.platform.service.extraction.batch;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for batch data writers
 * Supports multiple output formats (Parquet, CSV, JSON)
 */
public interface BatchWriter extends AutoCloseable {

    /**
     * Write a batch of records
     *
     * @param batch List of records (each record is a Map of column -> value)
     * @param batchNumber Sequential batch number (0, 1, 2, ...)
     * @throws IOException if write fails
     */
    void writeBatch(List<Map<String, Object>> batch, long batchNumber) throws IOException;

    /**
     * Flush any buffered data and finalize output
     *
     * @throws IOException if flush fails
     */
    void flush() throws IOException;

    /**
     * Get total bytes written
     */
    long getBytesWritten();

    /**
     * Get total records written
     */
    long getRecordsWritten();

    /**
     * Get output file path or URI
     */
    String getOutputLocation();
}
```

### Step 3: Implement Parquet Writer

**File:** `backend/src/main/java/com/jivs/platform/service/extraction/batch/ParquetBatchWriter.java`

```java
package com.jivs.platform.service.extraction.batch;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Writes extracted data to Parquet format
 *
 * Features:
 * - Columnar storage (10x compression vs CSV)
 * - Schema evolution support
 * - Snappy compression
 * - Native support in data warehouses
 */
public class ParquetBatchWriter implements BatchWriter {

    private static final Logger log = LoggerFactory.getLogger(ParquetBatchWriter.class);

    private final String outputPath;
    private final Schema schema;
    private ParquetWriter<GenericRecord> writer;
    private long recordsWritten = 0;
    private long bytesWritten = 0;

    public ParquetBatchWriter(String baseOutputPath, Map<String, Object> sampleRecord) throws IOException {
        this.outputPath = baseOutputPath + ".parquet";
        this.schema = inferSchema(sampleRecord);
        this.writer = createWriter();

        log.info("Parquet writer initialized: {}", outputPath);
    }

    @Override
    public void writeBatch(List<Map<String, Object>> batch, long batchNumber) throws IOException {
        log.debug("Writing Parquet batch {} with {} records", batchNumber, batch.size());

        for (Map<String, Object> record : batch) {
            GenericRecord avroRecord = convertToAvroRecord(record);
            writer.write(avroRecord);
            recordsWritten++;
        }

        // Estimate bytes (Parquet compresses, so this is approximate)
        bytesWritten += estimateBatchSize(batch);

        log.debug("Batch {} written successfully ({} records total)", batchNumber, recordsWritten);
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.close();
            log.info("Parquet file finalized: {} ({} records, ~{} bytes)",
                    outputPath, recordsWritten, bytesWritten);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public long getRecordsWritten() {
        return recordsWritten;
    }

    @Override
    public String getOutputLocation() {
        return outputPath;
    }

    /**
     * Infer Avro schema from sample record
     */
    private Schema inferSchema(Map<String, Object> sampleRecord) {
        StringBuilder schemaJson = new StringBuilder();
        schemaJson.append("{\"type\":\"record\",\"name\":\"ExtractionRecord\",\"fields\":[");

        boolean first = true;
        for (Map.Entry<String, Object> entry : sampleRecord.entrySet()) {
            if (!first) schemaJson.append(",");
            first = false;

            String fieldName = sanitizeFieldName(entry.getKey());
            String fieldType = inferAvroType(entry.getValue());

            schemaJson.append(String.format(
                "{\"name\":\"%s\",\"type\":[\"%s\",\"null\"],\"default\":null}",
                fieldName, fieldType
            ));
        }

        schemaJson.append("]}");

        log.debug("Inferred Avro schema: {}", schemaJson);
        return new Schema.Parser().parse(schemaJson.toString());
    }

    /**
     * Sanitize field names for Avro (alphanumeric + underscore only)
     */
    private String sanitizeFieldName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Infer Avro type from Java value
     */
    private String inferAvroType(Object value) {
        if (value == null) return "null";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof Double || value instanceof Float) return "double";
        if (value instanceof Boolean) return "boolean";
        return "string"; // Default to string
    }

    /**
     * Convert Map to Avro GenericRecord
     */
    private GenericRecord convertToAvroRecord(Map<String, Object> map) {
        GenericRecord record = new GenericData.Record(schema);

        for (Schema.Field field : schema.getFields()) {
            String originalKey = field.name();
            Object value = map.get(originalKey);

            // Handle type conversion
            if (value != null) {
                record.put(field.name(), convertValue(value, field.schema()));
            }
        }

        return record;
    }

    /**
     * Convert value to match Avro schema type
     */
    private Object convertValue(Object value, Schema fieldSchema) {
        if (value == null) return null;

        // Handle union types (e.g., ["string", "null"])
        Schema actualSchema = fieldSchema;
        if (fieldSchema.getType() == Schema.Type.UNION) {
            actualSchema = fieldSchema.getTypes().stream()
                    .filter(s -> s.getType() != Schema.Type.NULL)
                    .findFirst()
                    .orElse(fieldSchema);
        }

        // Convert based on target type
        switch (actualSchema.getType()) {
            case INT:
                return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
            case LONG:
                return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
            case DOUBLE:
                return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
            case BOOLEAN:
                return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
            default:
                return value.toString();
        }
    }

    /**
     * Create Parquet writer with optimal settings
     */
    private ParquetWriter<GenericRecord> createWriter() throws IOException {
        Configuration conf = new Configuration();

        // Hadoop filesystem configuration
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        return AvroParquetWriter.<GenericRecord>builder(new Path(outputPath))
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)  // Fast compression
                .withRowGroupSize(128 * 1024 * 1024)  // 128 MB row groups
                .withPageSize(1 * 1024 * 1024)  // 1 MB pages
                .withDictionaryEncoding(true)  // Enable dictionary encoding
                .build();
    }

    /**
     * Estimate batch size in bytes (before compression)
     */
    private long estimateBatchSize(List<Map<String, Object>> batch) {
        long size = 0;
        for (Map<String, Object> record : batch) {
            for (Object value : record.values()) {
                if (value != null) {
                    size += value.toString().getBytes().length;
                }
            }
        }
        return size / 3; // Assume 3x compression ratio for Parquet
    }
}
```

### Step 4: Implement CSV Writer

**File:** `backend/src/main/java/com/jivs/platform/service/extraction/batch/CsvBatchWriter.java`

```java
package com.jivs.platform.service.extraction.batch;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Writes extracted data to CSV format
 *
 * Features:
 * - Excel-compatible
 * - Human-readable
 * - Universal format support
 */
public class CsvBatchWriter implements BatchWriter {

    private static final Logger log = LoggerFactory.getLogger(CsvBatchWriter.class);

    private final String outputPath;
    private final CSVWriter writer;
    private final List<String> columnOrder;
    private long recordsWritten = 0;
    private long bytesWritten = 0;
    private boolean headerWritten = false;

    public CsvBatchWriter(String baseOutputPath, Map<String, Object> sampleRecord) throws IOException {
        this.outputPath = baseOutputPath + ".csv";
        this.writer = new CSVWriter(new FileWriter(outputPath));
        this.columnOrder = new ArrayList<>(sampleRecord.keySet());

        log.info("CSV writer initialized: {} ({} columns)", outputPath, columnOrder.size());
    }

    @Override
    public void writeBatch(List<Map<String, Object>> batch, long batchNumber) throws IOException {
        log.debug("Writing CSV batch {} with {} records", batchNumber, batch.size());

        // Write header on first batch
        if (!headerWritten) {
            writeHeader();
            headerWritten = true;
        }

        // Write records
        for (Map<String, Object> record : batch) {
            String[] row = new String[columnOrder.size()];
            for (int i = 0; i < columnOrder.size(); i++) {
                Object value = record.get(columnOrder.get(i));
                row[i] = value != null ? value.toString() : "";
            }
            writer.writeNext(row);
            recordsWritten++;
            bytesWritten += estimateRowSize(row);
        }

        writer.flush();
        log.debug("Batch {} written ({} records total)", batchNumber, recordsWritten);
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            log.info("CSV file finalized: {} ({} records, {} bytes)",
                    outputPath, recordsWritten, bytesWritten);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public long getRecordsWritten() {
        return recordsWritten;
    }

    @Override
    public String getOutputLocation() {
        return outputPath;
    }

    private void writeHeader() throws IOException {
        writer.writeNext(columnOrder.toArray(new String[0]));
        bytesWritten += estimateRowSize(columnOrder.toArray(new String[0]));
    }

    private long estimateRowSize(String[] row) {
        long size = 0;
        for (String cell : row) {
            size += cell != null ? cell.getBytes().length : 0;
        }
        return size + row.length; // Add commas and newline
    }
}
```

### Step 5: Implement JSON Lines Writer

**File:** `backend/src/main/java/com/jivs/platform/service/extraction/batch/JsonLinesBatchWriter.java`

```java
package com.jivs.platform.service.extraction.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Writes extracted data to JSON Lines format (newline-delimited JSON)
 *
 * Features:
 * - One JSON object per line
 * - Streamable format
 * - Developer-friendly
 */
public class JsonLinesBatchWriter implements BatchWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonLinesBatchWriter.class);

    private final String outputPath;
    private final BufferedWriter writer;
    private final ObjectMapper objectMapper;
    private long recordsWritten = 0;
    private long bytesWritten = 0;

    public JsonLinesBatchWriter(String baseOutputPath) throws IOException {
        this.outputPath = baseOutputPath + ".jsonl";
        this.writer = new BufferedWriter(new FileWriter(outputPath));
        this.objectMapper = new ObjectMapper();

        log.info("JSON Lines writer initialized: {}", outputPath);
    }

    @Override
    public void writeBatch(List<Map<String, Object>> batch, long batchNumber) throws IOException {
        log.debug("Writing JSON Lines batch {} with {} records", batchNumber, batch.size());

        for (Map<String, Object> record : batch) {
            String json = objectMapper.writeValueAsString(record);
            writer.write(json);
            writer.newLine();
            recordsWritten++;
            bytesWritten += json.getBytes().length + 1; // +1 for newline
        }

        writer.flush();
        log.debug("Batch {} written ({} records total)", batchNumber, recordsWritten);
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            log.info("JSON Lines file finalized: {} ({} records, {} bytes)",
                    outputPath, recordsWritten, bytesWritten);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public long getRecordsWritten() {
        return recordsWritten;
    }

    @Override
    public String getOutputLocation() {
        return outputPath;
    }
}
```

### Step 6: Create Writer Factory

**File:** `backend/src/main/java/com/jivs/platform/service/extraction/batch/BatchWriterFactory.java`

```java
package com.jivs.platform.service.extraction.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Factory for creating appropriate batch writers
 */
@Component
public class BatchWriterFactory {

    private static final Logger log = LoggerFactory.getLogger(BatchWriterFactory.class);

    public enum OutputFormat {
        PARQUET, CSV, JSON_LINES
    }

    /**
     * Create batch writer based on output format
     *
     * @param format Output format
     * @param outputPath Base output path (extension will be added)
     * @param sampleRecord Sample record for schema inference
     * @return BatchWriter instance
     */
    public BatchWriter createWriter(OutputFormat format, String outputPath,
                                   Map<String, Object> sampleRecord) throws IOException {

        log.info("Creating {} writer for output: {}", format, outputPath);

        switch (format) {
            case PARQUET:
                return new ParquetBatchWriter(outputPath, sampleRecord);

            case CSV:
                return new CsvBatchWriter(outputPath, sampleRecord);

            case JSON_LINES:
                return new JsonLinesBatchWriter(outputPath);

            default:
                throw new IllegalArgumentException("Unsupported output format: " + format);
        }
    }

    /**
     * Parse format from string parameter
     */
    public OutputFormat parseFormat(String formatStr) {
        if (formatStr == null || formatStr.trim().isEmpty()) {
            return OutputFormat.PARQUET; // Default
        }

        try {
            return OutputFormat.valueOf(formatStr.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown format '{}', defaulting to PARQUET", formatStr);
            return OutputFormat.PARQUET;
        }
    }
}
```

### Step 7: Update JdbcConnector to Use Writers

**File:** Update `JdbcConnector.java` and `PooledJdbcConnector.java`

```java
// Add field
private BatchWriter batchWriter;
private Map<String, Object> firstRecord; // For schema inference

// In extract() method, before while loop:
String format = parameters.getOrDefault("format", "parquet");
BatchWriterFactory.OutputFormat outputFormat =
    batchWriterFactory.parseFormat(format);

// In while loop, after extracting first record:
if (totalRecords == 1 && batchWriter == null) {
    // Initialize writer with first record for schema
    firstRecord = new HashMap<>(record);
    batchWriter = batchWriterFactory.createWriter(
        outputFormat,
        outputPath,
        firstRecord
    );
}

// Replace processBatch() method:
private void processBatch(List<Map<String, Object>> batch, String outputPath, long batchNumber) {
    try {
        if (batchWriter != null) {
            batchWriter.writeBatch(batch, batchNumber);
            log.debug("Batch {} written successfully", batchNumber);
        } else {
            log.warn("Batch writer not initialized - data will be lost");
        }
    } catch (IOException e) {
        log.error("Failed to write batch {}", batchNumber, e);
        throw new RuntimeException("Batch write failed", e);
    }
}

// In finally block:
if (batchWriter != null) {
    try {
        batchWriter.flush();
        result.setBytesProcessed(batchWriter.getBytesWritten());
        result.setOutputPath(batchWriter.getOutputLocation());
        log.info("Extraction output written to: {}", batchWriter.getOutputLocation());
    } catch (IOException e) {
        log.error("Failed to finalize batch writer", e);
    }
}
```

---

## ✅ TESTING

### Unit Test

**File:** `backend/src/test/java/com/jivs/platform/service/extraction/batch/ParquetBatchWriterTest.java`

```java
@Test
void testParquetBatchWriting() throws IOException {
    // Sample data
    Map<String, Object> sampleRecord = Map.of(
        "id", 1L,
        "name", "John Doe",
        "email", "john@example.com",
        "created_at", "2024-01-01"
    );

    List<Map<String, Object>> batch = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        batch.add(Map.of(
            "id", (long) i,
            "name", "User " + i,
            "email", "user" + i + "@example.com",
            "created_at", "2024-01-01"
        ));
    }

    // Create writer
    String outputPath = "/tmp/test_extraction";
    ParquetBatchWriter writer = new ParquetBatchWriter(outputPath, sampleRecord);

    // Write batch
    writer.writeBatch(batch, 0);
    writer.flush();

    // Verify
    assertEquals(1000, writer.getRecordsWritten());
    assertTrue(writer.getBytesWritten() > 0);
    assertTrue(new File(outputPath + ".parquet").exists());

    // Verify file can be read
    // (Would use Parquet reader library here)
}
```

---

## 📋 DEPLOYMENT CHECKLIST

- [ ] Add Maven dependencies
- [ ] Create batch writer classes
- [ ] Update JdbcConnector and PooledJdbcConnector
- [ ] Add unit tests
- [ ] Add integration test (end-to-end extraction)
- [ ] Configure output directory permissions
- [ ] Test with 1M+ records
- [ ] Verify file compression ratios
- [ ] Document output formats in API docs

**Estimated Effort:** 10 hours
**Priority:** P0 - CRITICAL

---

**Related Documents:**
- `EXTRACTION_MODULE_FIXES.md` - Other critical fixes
- `EXTRACTION_MODULE_AUDIT_REPORT.md` - Full audit
