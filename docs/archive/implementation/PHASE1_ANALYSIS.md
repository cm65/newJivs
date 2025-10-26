# Phase 1 Analysis: DocumentArchivingService

**Date**: October 21, 2025, 22:30
**Status**: ✅ Analysis Complete

---

## Critical Findings

### ⚠️ MAJOR PROBLEM: Most Methods Are Stubs

**Lines with TODO (Not Implemented)**:
- Line 403-406: `getDocumentData()` - Returns `new byte[0]` (STUB!)
- Line 373-379: `storeInArchive()` - Returns fake path (STUB!)
- Line 384-389: `retrieveFromArchive()` - Returns `new byte[0]` (STUB!)
- Line 394-398: `deleteFromArchive()` - Does nothing (STUB!)
- Line 411-413: `storeInActiveStorage()` - Does nothing (STUB!)
- Line 418-421: `updateDocumentStatus()` - Does nothing (STUB!)
- Line 454-457: `storeArchiveRecord()` - Does nothing (STUB!)
- Line 462-466: `getArchiveRecord()` - Returns `null` (STUB!)
- Line 471-474: `deleteArchiveRecord()` - Does nothing (STUB!)

**What This Means**: The DocumentArchivingService is a **SKELETON**. Only compression/decompression work!

---

## What Actually Works

### ✅ Working Methods:
1. `compress(byte[] data)` (lines 338-347) - ZIP compression
2. `decompress(byte[] compressedData)` (lines 352-368) - ZIP decompression
3. `calculateChecksum(byte[] data)` (lines 440-449) - SHA-256 checksum
4. `generateMetadata()` (lines 426-435) - Creates metadata object

### ❌ Not Working (All Stubs):
- **ALL storage operations** (get, store, delete)
- **ALL database operations** (updateStatus, storeRecord, getRecord)
- **ALL tier management**

---

## Dependencies Analysis

### Required Services:
1. **StorageService** (injected, line 28)
   - Currently NOT used in any meaningful way
   - Need to use it for actual file I/O

2. **DocumentRepository** (NOT injected!)
   - Need to inject to update document status
   - Missing dependency!

3. **DocumentService** (NOT injected!)
   - Need to inject to get/update documents
   - Missing dependency!

---

## Data Type Mismatches

### Problem: ArchiveRequest.documentId is String
```java
private String documentId;  // Line 497
```

### But Our Documents Use Long IDs:
```java
// Document entity uses Long id
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**Impact**: Need to convert between String and Long everywhere

---

## Implementation Required (7 Critical Methods)

### 1. getDocumentData(String documentId)
**Current**: Returns `new byte[0]`
**Need**:
```java
private byte[] getDocumentData(String documentId) throws IOException {
    Long id = Long.parseLong(documentId);
    DocumentDTO doc = documentService.getDocument(id);
    if (doc == null) {
        throw new IllegalArgumentException("Document not found: " + documentId);
    }
    Path filePath = Paths.get(doc.getStoragePath());
    if (!Files.exists(filePath)) {
        throw new FileNotFoundException("File not found: " + doc.getStoragePath());
    }
    return Files.readAllBytes(filePath);
}
```

---

### 2. storeInArchive(String archiveId, byte[] data, StorageTier tier)
**Current**: Returns fake path `"archives/" + tier + "/" + archiveId`
**Need**:
```java
private String storeInArchive(String archiveId, byte[] data, StorageTier tier) throws IOException {
    // archiveId is actually documentId for our simpler approach
    Long documentId = Long.parseLong(archiveId);
    DocumentDTO doc = documentService.getDocument(documentId);

    // Overwrite existing file with compressed version
    Path filePath = Paths.get(doc.getStoragePath());
    Files.write(filePath, data);

    log.info("Stored compressed archive: {} ({} bytes)", archiveId, data.length);
    return filePath.toString();
}
```

---

### 3. updateDocumentStatus(String documentId, DocumentStatus status)
**Current**: Does nothing
**Need**:
```java
private void updateDocumentStatus(String documentId, DocumentStatus status) {
    Long id = Long.parseLong(documentId);
    DocumentDTO doc = documentService.getDocument(id);
    if (doc != null) {
        doc.setArchived(status == DocumentStatus.ARCHIVED);
        doc.setCompressed(true);  // Mark as compressed
        documentService.updateDocument(id, doc);
        log.info("Updated document {} status to {}", documentId, status);
    }
}
```

---

### 4. Add Missing Dependency Injections
**Need to add**:
```java
private final StorageService storageService;  // Already exists
private final DocumentService documentService;  // MISSING - need to add
private final DocumentRepository documentRepository;  // MISSING - need to add
```

---

## Simplified Approach (Decision)

### Instead of Complex Archive System:
**Original design**: Separate archive storage, archive records table, complex tracking

### We'll Use Simpler Approach:
**Simpler**: Compress file in-place, update document metadata

**Why**:
1. Don't need separate archive storage (use existing storage)
2. Don't need archive_records table (use documents table)
3. Document.compressed field tracks compression
4. Faster to implement
5. Easier to test
6. Less complex

**Trade-off**:
- Lose separate archive tracking
- Lose archive history
- But GAIN: Working compression NOW

---

## Revised Implementation Strategy

### What We'll Do:
1. **Inject DocumentService** into DocumentArchivingService
2. **Implement getDocumentData()** - read from existing storage path
3. **Implement storeInArchive()** - overwrite with compressed version
4. **Implement updateDocumentStatus()** - set archived=true, compressed=true
5. **Skip archive_records** - not needed for MVP
6. **Skip storage tier migration** - use later

### What We Won't Do (For Now):
- ❌ Create archive_records table
- ❌ Implement retrieveFromArchive() (use regular download)
- ❌ Implement tier migration
- ❌ Implement archive search

---

## Dependencies Checklist

- [ ] Add DocumentService injection
- [ ] Add DocumentRepository injection (if needed)
- [ ] Import Long type conversions
- [ ] Import Files, Path, Paths
- [ ] Import FileNotFoundException

---

## Type Conversion Strategy

### Problem: documentId is String everywhere in ArchiveRequest/Result
### Solution: Convert at boundaries

```java
// When building ArchiveRequest
request.setDocumentId(String.valueOf(doc.getId()));  // Long → String

// When using documentId
Long id = Long.parseLong(request.getDocumentId());  // String → Long
```

---

## Testing Implications

### Before Phase 4 Testing:
1. Must inject DocumentService
2. Must implement 3 core methods
3. Must handle type conversions
4. Must test with real document in database

### Cannot Test Until:
- DocumentService properly injected
- Methods actually implemented (not stubs)
- Database has a test document

---

## Risk Assessment

### Low Risk:
- ✅ Compression/decompression already work
- ✅ No new tables needed
- ✅ Using existing storage paths

### Medium Risk:
- ⚠️ Type conversion Long ↔ String
- ⚠️ File overwrite (what if fails mid-write?)
- ⚠️ Async @Async annotation (might cause issues)

### High Risk:
- ❌ None identified

---

## Decision: Remove @Async for Now

**Problem**: `@Async` on archiveDocument() returns CompletableFuture
**Issue**: Harder to test, more error handling needed
**Decision**: Make it synchronous for MVP

**Change**:
```java
// Before
@Async
public CompletableFuture<ArchiveResult> archiveDocument(ArchiveRequest request)

// After
public ArchiveResult archiveDocument(ArchiveRequest request)
```

**Why**: Simpler, easier to test, blocking is fine for single-document archive

---

## Next Steps (Phase 1.2)

1. Map all code paths that call archiving
2. Identify what frontend expects
3. Check DocumentController current implementation
4. Verify Document entity has compressed field

**Status**: Ready to proceed to Phase 1.2

---

**Analysis Complete**: October 21, 2025, 22:35
**Duration**: 5 minutes
**Findings**: 9 stub methods, 2 missing dependencies, type mismatch, need simplified approach
**Recommendation**: Proceed with simplified in-place compression approach
