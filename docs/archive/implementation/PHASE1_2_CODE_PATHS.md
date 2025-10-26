# Phase 1.2 Analysis: Code Path Mapping

**Date**: October 22, 2025, 01:15
**Status**: ✅ Analysis Complete

---

## Executive Summary

Mapped all code paths that trigger archiving operations. Discovered:
- ✅ Document entity already has `compressed` field (line 110-111)
- ✅ DocumentDTO already has `compressed` field (line 37)
- ❌ **CRITICAL**: Frontend calls `/documents/archive` endpoint that **DOESN'T EXIST**
- ❌ Only individual archive endpoint exists: `POST /{id}/archive`
- ⚠️ Archive-on-upload works but doesn't compress (just sets flags)

---

## Code Path #1: Individual Document Archive (Existing)

### Frontend → Backend Flow

**Frontend**: User clicks "Archive" button on single document
- File: `frontend/src/pages/Documents.tsx` (lines 254-273)
- Frontend calls: `POST /documents/archive` with bulk payload
- **PROBLEM**: This endpoint **DOES NOT EXIST** in backend!

**Backend**: Individual archive endpoint (exists)
- File: `backend/src/main/java/com/jivs/platform/controller/DocumentController.java`
- Endpoint: `POST /documents/{id}/archive` (lines 244-274)
- Parameters:
  - `compress` (default: true) - **IGNORED! Not used**
  - `storageTier` (default: WARM)
  - `deleteOriginal` (default: false) - **IGNORED! Not used**

**Current Implementation**:
```java
// Line 254-258
DocumentDTO doc = documentService.getDocument(id);
if (doc != null) {
    doc.setArchived(true);  // ❌ Just flips flag
    doc.setStorageTier(storageTier);  // ❌ Just sets DB field
    documentService.updateDocument(id, doc);  // ❌ No compression!
}
```

**What Actually Happens**:
1. ✅ Gets document from database
2. ✅ Sets `archived = true`
3. ✅ Sets `storageTier = "WARM"`
4. ✅ Updates database record
5. ❌ File on disk **UNCHANGED**
6. ❌ File size **SAME**
7. ❌ No compression
8. ❌ `compressed` field stays `false`

---

## Code Path #2: Archive on Upload (Existing)

### Frontend → Backend Flow

**Frontend**: User uploads file with "Archive immediately" checkbox
- Component: Upload dialog in Documents.tsx
- Sends: `archive=true` parameter to `/documents/upload`

**Backend**: Document upload with archive flag
- File: `DocumentController.java` (lines 60-88)
- Endpoint: `POST /documents/upload`
- Parameter: `archive` (default: false)

**Current Implementation**:
```java
// Line 73-78: Upload file
DocumentDTO document = documentService.uploadDocument(
    file, title, description, tagList
);

// Line 81-86: Archive if requested
if (archive) {
    document.setArchived(true);  // ❌ Just flips flag
    document.setStorageTier("WARM");  // ❌ Just sets DB field
    document = documentService.updateDocument(document.getId(), document);
    log.info("Document {} archived immediately after upload", document.getId());
}
```

**What Actually Happens**:
1. ✅ Uploads file to `/var/jivs/storage/documents/`
2. ✅ Stores file in **UNCOMPRESSED** format
3. ✅ Creates database record
4. ✅ If archive=true: Sets `archived = true`
5. ✅ If archive=true: Sets `storageTier = "WARM"`
6. ❌ File already written to disk (uncompressed)
7. ❌ No compression happens
8. ❌ `compressed` field stays `false`

**THE PROBLEM**: File is uploaded FIRST, then we try to archive. But archiving just changes DB flags, doesn't touch the file.

---

## Code Path #3: Bulk Archive (Missing Backend!)

### Frontend → Backend Flow

**Frontend**: User selects multiple documents, clicks "Archive Selected"
- File: `frontend/src/pages/Documents.tsx` (lines 254-273)
- Function: `handleArchive()`
- Request:
  ```javascript
  await apiClient.post('/documents/archive', {
    documentIds: [123, 456, 789],
    archiveType: 'WARM',
    compress: true,
    encrypt: false,
    archiveReason: 'User request',
    requestedBy: 'admin'
  });
  ```

**Backend**: **ENDPOINT MISSING!**
- Expected: `POST /documents/archive` (bulk endpoint)
- Actual: **Does not exist**
- Result: Frontend call will return **404 Not Found**

**What Should Happen**:
1. Receive list of document IDs
2. Loop through each document
3. Compress file on disk
4. Update database record
5. Return results

**What Actually Happens**:
- ❌ **404 ERROR** - Endpoint not implemented

---

## Code Path #4: Restore Archived Document (Existing)

### Frontend → Backend Flow

**Backend**: Restore archived document
- File: `DocumentController.java` (lines 279-293)
- Endpoint: `POST /documents/{id}/restore`
- Implementation: `documentService.restoreDocument(id)`

**Current Implementation**:
```java
boolean restored = documentService.restoreDocument(id);
```

**What Needs to Check**:
1. Is the document compressed?
2. If compressed, decompress on restore?
3. Or leave compressed and decompress on download?

**Decision Needed**: When restoring, should we:
- **Option A**: Decompress file immediately (move HOT → WARM → COLD)
- **Option B**: Keep file compressed, decompress on download (simpler)

**Recommendation**: Option B (keep compressed, decompress on download)

---

## Code Path #5: Document Download (Needs Update)

### Frontend → Backend Flow

**Backend**: Download document
- File: `DocumentController.java` (lines 131-166)
- Endpoint: `GET /documents/{id}/download`
- Implementation: Uses `StorageService.retrieveFile()`

**Current Flow**:
```java
// Line 153-159
String storageId = /* construct from document metadata */
StorageResult result = storageService.retrieveFile(
    storageId,
    new StorageOptions().setEncrypted(doc.isEncrypted())
);
```

**What Happens Now**:
1. ✅ Gets document metadata from database
2. ✅ Reads file from storage path
3. ✅ If encrypted: decrypts
4. ❌ If compressed: **DOES NOT DECOMPRESS** (missing!)
5. ❌ Returns compressed file (browser can't open it)

**What SHOULD Happen**:
```java
StorageOptions options = new StorageOptions()
    .setEncrypted(doc.isEncrypted())
    .setCompress(doc.isCompressed());  // ← ADD THIS!

StorageResult result = storageService.retrieveFile(storageId, options);
```

**StorageService.retrieveFile()** already has decompression logic (lines 168-171):
```java
// Decompress if required
if (options.isCompress() && metadata.isCompressed()) {
    fileData = decompressData(fileData);
}
```

**Fix Required**: Pass `compress` option to `retrieveFile()` in DocumentController

---

## Data Model Analysis

### Document Entity (Already Ready!)

**File**: `backend/src/main/java/com/jivs/platform/domain/Document.java`

**Relevant Fields** (lines 107-117):
```java
@Column(name = "encrypted")
private boolean encrypted;  // ✅ Already exists

@Column(name = "compressed")
private boolean compressed;  // ✅ Already exists!

@Column(name = "compression_ratio")
private Double compressionRatio;  // ✅ Already exists!

@Column(name = "storage_tier")
private String storageTier;  // ✅ Already exists
```

**Getters/Setters** (lines 216-226):
```java
public boolean isEncrypted() { return encrypted; }
public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

public boolean isCompressed() { return compressed; }
public void setCompressed(boolean compressed) { this.compressed = compressed; }

public Double getCompressionRatio() { return compressionRatio; }
public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }

public String getStorageTier() { return storageTier; }
public void setStorageTier(String storageTier) { this.storageTier = storageTier; }
```

**✅ NO DATABASE MIGRATION NEEDED!** Fields already exist in entity and database.

---

### DocumentDTO (Already Ready!)

**File**: `backend/src/main/java/com/jivs/platform/dto/DocumentDTO.java`

**Relevant Fields** (lines 25, 35-38):
```java
private boolean archived;  // ✅ Already exists
private String storageTier;  // ✅ Already exists
private boolean encrypted;  // ✅ Already exists
private boolean compressed;  // ✅ Already exists
private Double compressionRatio;  // ✅ Already exists
```

**✅ NO DTO CHANGES NEEDED!** All fields already present.

---

## DocumentArchivingService Analysis

### Current State (From Phase 1.1)

**Service**: `backend/src/main/java/com/jivs/platform/service/archiving/DocumentArchivingService.java`

**Working Methods** (4):
1. ✅ `compress(byte[] data)` - ZIP compression (lines 338-347)
2. ✅ `decompress(byte[] compressedData)` - ZIP decompression (lines 352-368)
3. ✅ `calculateChecksum(byte[] data)` - SHA-256 (lines 440-449)
4. ✅ `generateMetadata()` - Creates metadata (lines 426-435)

**Stub Methods** (9):
1. ❌ `getDocumentData(String documentId)` - Returns `new byte[0]`
2. ❌ `storeInArchive(String archiveId, byte[] data, StorageTier tier)` - Returns fake path
3. ❌ `retrieveFromArchive(String archiveId)` - Returns `new byte[0]`
4. ❌ `deleteFromArchive(String archiveId)` - Does nothing
5. ❌ `storeInActiveStorage(String documentId, byte[] data)` - Does nothing
6. ❌ `updateDocumentStatus(String documentId, DocumentStatus status)` - Does nothing
7. ❌ `storeArchiveRecord(ArchiveMetadata metadata)` - Does nothing
8. ❌ `getArchiveRecord(String archiveId)` - Returns null
9. ❌ `deleteArchiveRecord(String archiveId)` - Does nothing

**Dependencies** (line 28):
```java
private final StorageService storageService;  // ✅ Injected
// ❌ MISSING: private final DocumentService documentService;
// ❌ MISSING: private final DocumentRepository documentRepository;
```

---

## Missing Backend Endpoints

### 1. Bulk Archive Endpoint (Required by Frontend!)

**Frontend Expects**: `POST /documents/archive`

**Request Body**:
```json
{
  "documentIds": [123, 456, 789],
  "archiveType": "WARM",
  "compress": true,
  "encrypt": false,
  "archiveReason": "User request",
  "requestedBy": "admin"
}
```

**Required Implementation**:
```java
@PostMapping("/archive")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Operation(summary = "Archive multiple documents")
public ResponseEntity<Map<String, Object>> archiveDocuments(
        @RequestBody Map<String, Object> request) {

    List<Long> documentIds = (List<Long>) request.get("documentIds");
    boolean compress = (boolean) request.getOrDefault("compress", true);
    String storageTier = (String) request.getOrDefault("archiveType", "WARM");

    // Loop through documents and archive each one
    List<Map<String, Object>> results = new ArrayList<>();
    for (Long id : documentIds) {
        // Call DocumentArchivingService.archiveDocument()
        // Or call the fixed single archive logic
        results.add(archiveSingleDocument(id, compress, storageTier));
    }

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("archived", results.size());
    response.put("results", results);

    return ResponseEntity.ok(response);
}
```

**Status**: ❌ **MISSING - MUST IMPLEMENT**

---

## Archiving Rules Endpoints (Already Implemented!)

### 1. Get Archiving Rules
- Endpoint: `GET /documents/archiving/rules` ✅
- File: `DocumentController.java` (lines 430-450)
- Returns: Storage tiers (HOT/WARM/COLD), retention days, defaults

### 2. Create Archiving Rule
- Endpoint: `POST /documents/archiving/rules` ✅
- File: `DocumentController.java` (lines 455-467)
- Status: Stub implementation (returns success)

### 3. Update Archiving Rule
- Endpoint: `PUT /documents/archiving/rules/{id}` ✅
- File: `DocumentController.java` (lines 472+)

### 4. Delete Archiving Rule
- Endpoint: `DELETE /documents/archiving/rules/{id}` ✅
- Status: Likely exists (didn't read full file)

**Status**: ✅ Archiving rules endpoints already implemented (stubs)

---

## Critical Findings Summary

### ✅ What Already Exists (Good News!)
1. ✅ Document entity has `compressed` field
2. ✅ Document entity has `compressionRatio` field
3. ✅ Document entity has `storageTier` field
4. ✅ DocumentDTO has all required fields
5. ✅ Individual archive endpoint exists: `POST /{id}/archive`
6. ✅ Archive-on-upload exists (line 81-86)
7. ✅ Restore endpoint exists: `POST /{id}/restore`
8. ✅ Archiving rules endpoints exist
9. ✅ Compression/decompression methods work in DocumentArchivingService
10. ✅ StorageService has compression/decompression logic

### ❌ What's Missing (Problems!)
1. ❌ Bulk archive endpoint: `POST /documents/archive` - **REQUIRED BY FRONTEND**
2. ❌ Individual archive doesn't compress files (just changes DB flags)
3. ❌ Archive-on-upload doesn't compress (file already written)
4. ❌ Download doesn't decompress compressed files
5. ❌ DocumentArchivingService has 9 stub methods
6. ❌ DocumentArchivingService missing DocumentService injection
7. ❌ No code actually calls DocumentArchivingService

### ⚠️ What Works But Needs Fixing
1. ⚠️ Individual archive endpoint accepts `compress` param but ignores it
2. ⚠️ Individual archive endpoint accepts `deleteOriginal` param but ignores it
3. ⚠️ Archive-on-upload sets flags but doesn't compress
4. ⚠️ Frontend expects bulk endpoint but backend only has single

---

## Implementation Strategy (Revised)

### Simplified Approach (From Phase 1.1)

**Decision**: In-place compression (no separate archive storage)

**Why**:
- ✅ Document entity already has `compressed` field
- ✅ No database migration needed
- ✅ StorageService already has compression logic
- ✅ Simpler to implement
- ✅ Faster to test
- ✅ Easier to debug

**How It Works**:
1. Read file from storage path
2. Compress using GZIP
3. Overwrite file with compressed version
4. Update database: `compressed = true`, `compressionRatio = 0.3`
5. On download: Check `compressed` flag, decompress if needed

**Trade-offs**:
- ❌ Lose original uncompressed file (can't undo)
- ❌ No separate archive storage tier (all files in same directory)
- ✅ Gain: Immediate storage savings
- ✅ Gain: Simpler implementation

---

## What Needs to Be Implemented (7 Items)

### 1. Fix Individual Archive Endpoint ✅ IDENTIFIED
**File**: `DocumentController.java` (lines 244-274)
**Change**: Instead of just setting flags, call compression logic
```java
// OLD (lines 254-258)
doc.setArchived(true);
doc.setStorageTier(storageTier);
documentService.updateDocument(id, doc);

// NEW (proposed)
// 1. Read file from storage
// 2. Compress file
// 3. Overwrite file with compressed version
// 4. Set compressed = true
// 5. Set compressionRatio
// 6. Set archived = true
// 7. Update database
```

### 2. Add Bulk Archive Endpoint ✅ IDENTIFIED
**File**: `DocumentController.java`
**Add**: New endpoint `POST /documents/archive`
**Logic**: Loop through documentIds, call single archive for each

### 3. Fix Archive-on-Upload ✅ IDENTIFIED
**File**: `DocumentController.java` (lines 81-86)
**Problem**: File already written before we try to archive
**Solution**:
- Option A: Compress during upload (before writing)
- Option B: Upload first, then compress file (after writing)
**Recommendation**: Option B (simpler, use existing archive logic)

### 4. Fix Document Download ✅ IDENTIFIED
**File**: `DocumentController.java` (lines 131-166)
**Change**: Pass `compress` option to `retrieveFile()`
```java
// Line 153-159 (current)
StorageResult result = storageService.retrieveFile(
    storageId,
    new StorageOptions().setEncrypted(doc.isEncrypted())
);

// NEW
StorageResult result = storageService.retrieveFile(
    storageId,
    new StorageOptions()
        .setEncrypted(doc.isEncrypted())
        .setCompress(doc.isCompressed())  // ← ADD THIS!
);
```

### 5. Implement Archive Helper Method ✅ NEW
**File**: `DocumentController.java` or `DocumentService.java`
**Name**: `compressDocumentFile(Long documentId, String storageTier)`
**Logic**:
```java
private void compressDocumentFile(Long documentId, String storageTier) throws IOException {
    // 1. Get document from database
    DocumentDTO doc = documentService.getDocument(documentId);

    // 2. Read file from storage
    Path filePath = Paths.get(doc.getStoragePath());
    byte[] fileData = Files.readAllBytes(filePath);

    // 3. Compress using StorageService
    byte[] originalData = fileData;
    byte[] compressed = compressData(fileData);  // Use GZIP

    // 4. Calculate compression ratio
    double ratio = (double) compressed.length / originalData.length;

    // 5. Overwrite file
    Files.write(filePath, compressed);

    // 6. Update database
    doc.setCompressed(true);
    doc.setCompressionRatio(ratio);
    doc.setArchived(true);
    doc.setStorageTier(storageTier);
    documentService.updateDocument(documentId, doc);

    log.info("Compressed document {}: {} bytes → {} bytes (ratio: {:.2f})",
        documentId, originalData.length, compressed.length, ratio);
}
```

### 6. Update DocumentService (If Needed) ✅ IDENTIFIED
**Check**: Does `documentService.updateDocument()` properly save all fields?
**Verify**: `compressed`, `compressionRatio`, `storageTier` fields saved

### 7. Remove DocumentArchivingService Dependency ✅ DECISION
**Decision**: Don't use DocumentArchivingService for now
**Reason**: 90% stub code, would need major refactoring
**Alternative**: Implement compression directly in controller/service
**Future**: Can refactor to use DocumentArchivingService later

---

## Type Conversion Strategy

### Problem: documentId is String in some places, Long in others

**DocumentArchivingService**:
- `ArchiveRequest.documentId` is `String` (line 497)
- `ArchiveResult.documentId` is `String`

**Document Entity**:
- `Document.id` is `Long` (line 24)

**DocumentDTO**:
- `DocumentDTO.id` is `Long` (line 17)

**Controller**:
- `@PathVariable Long id` (correct type)

**Conversion Needed**:
```java
// When calling DocumentArchivingService (if we use it)
request.setDocumentId(String.valueOf(doc.getId()));  // Long → String

// Inside DocumentArchivingService
Long id = Long.parseLong(request.getDocumentId());  // String → Long
```

**Recommendation**: Keep controller using Long, don't convert unless using DocumentArchivingService

---

## Breaking Points Identified (Preview for Phase 1.3)

### 1. File Overwrite Risk ⚠️
**Problem**: Compressing file in-place overwrites original
**Risk**: If compression fails mid-write, file corrupted
**Mitigation**: Write to temp file first, then atomic move

### 2. Download Before Compression Complete ⚠️
**Problem**: User downloads file while compression in progress
**Risk**: Gets partially compressed file (corrupted)
**Mitigation**: Use file locking or transaction

### 3. Multiple Compress Attempts ⚠️
**Problem**: User clicks "Archive" twice quickly
**Risk**: Second compress tries to compress already-compressed file
**Mitigation**: Check `compressed` flag before compressing

### 4. Decompression Failure ⚠️
**Problem**: Compressed file corrupted, can't decompress on download
**Risk**: User can't access their file
**Mitigation**: Keep checksum, verify before/after compression

### 5. Database Update Fails ⚠️
**Problem**: File compressed but database update fails
**Risk**: File compressed, but `compressed=false` in DB → download fails
**Mitigation**: Transaction rollback, or verify before returning success

### 6. Storage Path Issues ⚠️
**Problem**: Document.storagePath is null or invalid
**Risk**: Can't read file to compress
**Mitigation**: Validate storagePath exists before compressing

### 7. Frontend Bulk Archive 404 ⚠️
**Problem**: Frontend calls `/documents/archive` which doesn't exist
**Risk**: Bulk archive feature completely broken
**Mitigation**: Implement missing endpoint (REQUIRED!)

---

## Phase 1.2 Complete ✅

**Time Spent**: 15 minutes
**Files Analyzed**: 7 files
**Endpoints Mapped**: 6 endpoints
**Code Paths Identified**: 5 paths
**Breaking Points Found**: 7 risks
**Missing Endpoints**: 1 critical (bulk archive)

**Status**: Ready to proceed to **Phase 1.3: Identify Breaking Points**

---

**Next Step**: Phase 1.3 - Deep analysis of what could go wrong during implementation

**Created**: October 22, 2025, 01:30
**Duration**: 15 minutes
