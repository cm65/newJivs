# Bulk Operations Architecture

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Extractions.tsx Page                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │         useBulkSelection Hook                          │   │
│  │  • selectedIds: Set<string>                            │   │
│  │  • toggleSelection(id)                                 │   │
│  │  • selectAll(items[])                                  │   │
│  │  • clearSelection()                                    │   │
│  │  • isSelected(id): boolean                             │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  ┌────────────────────────────────────────────────────────┐   │
│  │      BulkActionsToolbar Component                      │   │
│  │  Rendered when: selectedCount > 0                      │   │
│  │  • Shows "X items selected"                            │   │
│  │  • Action buttons: Start, Stop, Export, Delete         │   │
│  │  • Clear selection button                              │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  ┌────────────────────────────────────────────────────────┐   │
│  │             Material-UI Table                           │   │
│  │  ┌──────────────────────────────────────────────┐     │   │
│  │  │ [√] Name    Source  Status  Records  Date     │     │   │
│  │  ├──────────────────────────────────────────────┤     │   │
│  │  │ [√] Ext 1   JDBC    Running  5000   12/01     │     │   │
│  │  │ [√] Ext 2   SAP     Pending  0      12/02     │     │   │
│  │  │ [ ] Ext 3   API     Complete 10000  12/03     │     │   │
│  │  └──────────────────────────────────────────────┘     │   │
│  │  Header checkbox: Select All / Deselect All            │   │
│  │  Row checkboxes: Toggle individual selection           │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  ┌────────────────────────────────────────────────────────┐   │
│  │      Bulk Action Handler Functions                     │   │
│  │  • handleBulkAction(action: string)                    │   │
│  │  • executeBulkAction(action: string, ids: string[])    │   │
│  │  • handleConfirmBulkAction()                           │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  ┌────────────────────────────────────────────────────────┐   │
│  │         extractionService.ts                           │   │
│  │  • bulkStart(ids[])                                    │   │
│  │  • bulkStop(ids[])                                     │   │
│  │  • bulkDelete(ids[])                                   │   │
│  │  • bulkExport(ids[])                                   │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│                     HTTP POST                                   │
│                          ↓                                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Backend Controller                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  POST /api/v1/extractions/bulk                                  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │         ExtractionController.bulkAction()              │   │
│  │  @RequestBody BulkActionRequest:                       │   │
│  │    • ids: ["uuid1", "uuid2", "uuid3"]                  │   │
│  │    • action: "start" | "stop" | "delete" | "export"    │   │
│  │                                                         │   │
│  │  Process each ID:                                       │   │
│  │    ┌───────────────────────────────────┐              │   │
│  │    │ for (id : ids) {                  │              │   │
│  │    │   try {                            │              │   │
│  │    │     switch(action) {               │              │   │
│  │    │       case "start":                │              │   │
│  │    │         startExtraction(id);       │              │   │
│  │    │         successfulIds.add(id);     │              │   │
│  │    │       case "delete":               │              │   │
│  │    │         deleteExtraction(id);      │              │   │
│  │    │         successfulIds.add(id);     │              │   │
│  │    │     }                              │              │   │
│  │    │   } catch (Exception e) {          │              │   │
│  │    │     failedIds.put(id, e.message);  │              │   │
│  │    │   }                                 │              │   │
│  │    │ }                                   │              │   │
│  │    └───────────────────────────────────┘              │   │
│  │                                                         │   │
│  │  Return BulkActionResponse:                            │   │
│  │    • status: "success" | "partial" | "failed"          │   │
│  │    • totalProcessed: 3                                 │   │
│  │    • successCount: 2                                   │   │
│  │    • failureCount: 1                                   │   │
│  │    • successfulIds: ["uuid1", "uuid2"]                 │   │
│  │    • failedIds: {"uuid3": "Already running"}           │   │
│  │    • processingTimeMs: 1250                            │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Frontend Dialogs                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │       Confirmation Dialog (for delete)                 │   │
│  │  "Are you sure you want to delete 3 extractions?"      │   │
│  │  "This action cannot be undone."                        │   │
│  │  [Cancel]  [Confirm]                                    │   │
│  └────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  ┌────────────────────────────────────────────────────────┐   │
│  │         Bulk Progress Dialog                           │   │
│  │  During processing:                                     │   │
│  │    "Processing..."                                      │   │
│  │    [==============    ] LinearProgress                  │   │
│  │                                                         │   │
│  │  After completion:                                      │   │
│  │    "Processed 3 extractions: 2 succeeded, 1 failed"    │   │
│  │    Total Processed: 3                                   │   │
│  │    Successful: 2                                        │   │
│  │    Failed: 1                                            │   │
│  │    Processing Time: 1250ms                              │   │
│  │                                                         │   │
│  │    Failed Items:                                        │   │
│  │    • uuid3: Already running                             │   │
│  │                                                         │   │
│  │  [Close]                                                │   │
│  │  (Auto-closes after 3s if success)                     │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## State Flow Diagram

```
┌─────────────────────┐
│  Initial State      │
│  selectedIds: {}    │
│  selectedCount: 0   │
└─────────────────────┘
          │
          │ User clicks checkbox
          ↓
┌─────────────────────┐
│  Selection Made     │
│  selectedIds: {1,2} │
│  selectedCount: 2   │
└─────────────────────┘
          │
          │ BulkActionsToolbar appears
          ↓
┌─────────────────────┐
│  User clicks action │
│  e.g., "Start"      │
└─────────────────────┘
          │
          │ Non-destructive: execute immediately
          │ Destructive (delete): show confirmation
          ↓
┌─────────────────────────────┐
│  Confirmation Dialog        │
│  (only for destructive)     │
│  User confirms or cancels   │
└─────────────────────────────┘
          │ User confirms
          ↓
┌─────────────────────────────┐
│  Bulk Progress Dialog       │
│  status: "processing"       │
│  Shows LinearProgress       │
└─────────────────────────────┘
          │
          │ API call completes
          ↓
┌─────────────────────────────┐
│  Bulk Progress Dialog       │
│  status: "success"          │
│  Shows results              │
│  Auto-closes after 3s       │
└─────────────────────────────┘
          │
          │ clearSelection()
          ↓
┌─────────────────────┐
│  Back to Initial    │
│  selectedIds: {}    │
│  selectedCount: 0   │
│  Table refreshes    │
└─────────────────────┘
```

## Data Flow

```
Frontend                    Backend                   Database
────────                   ────────                  ─────────

User selects rows
     │
     ├─> useBulkSelection
     │   stores IDs in Set
     │
User clicks "Start"
     │
     ├─> handleBulkAction("start")
     │
     ├─> extractionService.bulkStart(["id1","id2"])
     │
     │   HTTP POST /api/v1/extractions/bulk
     │   Body: { ids: ["id1","id2"], action: "start" }
     │        │
     │        └──────────────────> ExtractionController
     │                                   │
     │                                   ├─> for each ID
     │                                   │     startExtraction(id1) ─────> UPDATE extraction
     │                                   │     startExtraction(id2) ─────> UPDATE extraction
     │                                   │
     │                                   ├─> Track success/failures
     │                                   │
     │        BulkActionResponse <───────┘
     │        {
     │          status: "success",
     │          successfulIds: ["id1","id2"],
     │          failedIds: {}
     │        }
     │        │
     ├───────<┘
     │
     ├─> setBulkProgress(response)
     │
     ├─> clearSelection()
     │
     └─> loadExtractions()  ─────────────────────────────> SELECT * FROM extractions
                                                                │
                                    Updated data <───────────────┘
                                         │
         Table re-renders <──────────────┘
```

## Integration with Existing Features

```
┌──────────────────────────────────────────────────────────┐
│                    User Workflow                          │
└──────────────────────────────────────────────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    │                     │                      │
    ↓                     ↓                      ↓
┌─────────┐        ┌──────────┐        ┌──────────────┐
│ Search  │        │ Advanced │        │ Quick        │
│ (WF 6)  │        │ Filtering│        │ Filters      │
│         │        │ (WF 7)   │        │ (WF 7)       │
└─────────┘        └──────────┘        └──────────────┘
    │                     │                      │
    └─────────────────────┼──────────────────────┘
                          │
                          ↓
             ┌────────────────────────┐
             │ Filtered/Searched      │
             │ Results in Table       │
             └────────────────────────┘
                          │
                          ↓
             ┌────────────────────────┐
             │ User Selects Rows      │
             │ (Bulk Selection)       │
             └────────────────────────┘
                          │
                          ↓
             ┌────────────────────────┐
             │ Bulk Actions Toolbar   │
             │ Appears                │
             └────────────────────────┘
                          │
                          ↓
             ┌────────────────────────┐
             │ User Performs Bulk     │
             │ Action                 │
             └────────────────────────┘
                          │
                          ↓
             ┌────────────────────────┐
             │ Real-time Updates      │
             │ (WF 9)                 │
             │ Show Status Changes    │
             └────────────────────────┘
```

## Error Handling Flow

```
Bulk Operation Initiated
         │
         ├─> Process ID 1 ────> Success ────> Add to successfulIds
         │
         ├─> Process ID 2 ────> Success ────> Add to successfulIds
         │
         ├─> Process ID 3 ────> Failure ────> Add to failedIds
         │                                      with error message
         │
         └─> All processed
                  │
                  ↓
         ┌───────────────────┐
         │ Response Status   │
         │ • All success     │ ──> "success"
         │ • Some failed     │ ──> "partial"
         │ • All failed      │ ──> "failed"
         └───────────────────┘
                  │
                  ↓
         ┌───────────────────┐
         │ Progress Dialog   │
         │ Shows:            │
         │ • Success count   │
         │ • Failure count   │
         │ • Failed items    │
         │   with reasons    │
         └───────────────────┘
                  │
                  ↓
         User can:
         • Close dialog
         • Review failures
         • Retry failed items individually
```

## Performance Considerations

```
┌──────────────────────────────────────────────────────┐
│                  Frontend                             │
├──────────────────────────────────────────────────────┤
│ • Use Set for O(1) selection lookup                  │
│ • Only render toolbar when items selected            │
│ • Debounce bulk action triggers                      │
│ • Virtual scrolling for large result sets (future)   │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                   Backend                             │
├──────────────────────────────────────────────────────┤
│ • Sequential processing (simple, reliable)           │
│ • Future: Parallel processing with ExecutorService   │
│ • Future: Batch size limits (max 100 items)          │
│ • Future: Async processing with job queue            │
│ • Measure and return processing time                 │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                  Database                             │
├──────────────────────────────────────────────────────┤
│ • Each operation is a separate transaction           │
│ • Failed operations don't affect successful ones     │
│ • Future: Batch UPDATE operations                    │
│ • Future: Database-level bulk operations             │
└──────────────────────────────────────────────────────┘
```

## Security Considerations

```
┌──────────────────────────────────────────────────────┐
│              Authorization Checks                     │
├──────────────────────────────────────────────────────┤
│                                                       │
│  Frontend:                                            │
│  • BulkActionsToolbar only shows if user has perms   │
│  • Actions filtered based on user role               │
│                                                       │
│  Backend:                                             │
│  • @PreAuthorize("hasAnyRole('ADMIN','DATA_ENG')")   │
│  • Each individual operation checks permissions      │
│  • Failed permission checks added to failedIds       │
│                                                       │
│  Audit:                                               │
│  • Log bulk operation with user, timestamp, IDs      │
│  • Log each successful/failed operation              │
│  • Track who initiated bulk delete operations        │
│                                                       │
└──────────────────────────────────────────────────────┘
```

---

**Architecture Version**: 1.0
**Last Updated**: January 12, 2025
**Compatible With**: Workflows 6, 7, 9
**Status**: Production Ready
