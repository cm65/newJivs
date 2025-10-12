# Bulk Operations Integration Guide

## Overview

This guide shows how to integrate the bulk operations functionality into the Extractions and Migrations pages that already have Advanced Filtering (from Workflow 7).

## Files Already Created

1. ✅ `frontend/src/hooks/useBulkSelection.ts` - Hook for multi-select state management
2. ✅ `frontend/src/components/BulkActionsToolbar.tsx` - Toolbar component
3. ✅ `frontend/src/services/extractionService.ts` - Updated with bulk methods
4. ✅ `frontend/src/services/migrationService.ts` - Updated with bulk methods
5. ✅ `backend/.../controller/ExtractionController.java` - Added POST /bulk endpoint
6. ✅ `backend/.../controller/MigrationController.java` - Added POST /bulk endpoint
7. ✅ `backend/.../dto/BulkActionRequest.java` - Request DTO
8. ✅ `backend/.../dto/BulkActionResponse.java` - Response DTO

## Integration Steps for Extractions.tsx

### Step 1: Add Imports

```typescript
// Add these imports at the top
import { Checkbox, DialogContentText, LinearProgress } from '@mui/material';
import BulkActionsToolbar, { BulkActionPresets } from '../components/BulkActionsToolbar';
import useBulkSelection from '../hooks/useBulkSelection';
import { BulkActionResponse } from '../services/extractionService';
```

### Step 2: Add Bulk Selection State

```typescript
// Inside Extractions component, after existing state declarations
const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
const [bulkProgressOpen, setBulkProgressOpen] = useState(false);
const [bulkProgress, setBulkProgress] = useState<BulkActionResponse | null>(null);
const [pendingBulkAction, setPendingBulkAction] = useState<{ action: string; ids: string[] } | null>(null);

// Add bulk selection hook
const {
  selectedIds,
  selectedIdsArray,
  selectedCount,
  toggleSelection,
  selectAll,
  clearSelection,
  isSelected,
  isAllSelected,
  isSomeSelected,
} = useBulkSelection<Extraction>();
```

### Step 3: Add Bulk Action Handlers

```typescript
// Add these handler functions
const handleBulkAction = async (action: string) => {
  if (selectedCount === 0) return;

  // Show confirmation for destructive actions
  if (action === 'delete') {
    setPendingBulkAction({ action, ids: selectedIdsArray });
    setConfirmDialogOpen(true);
    return;
  }

  await executeBulkAction(action, selectedIdsArray);
};

const executeBulkAction = async (action: string, ids: string[]) => {
  try {
    setBulkProgressOpen(true);
    setBulkProgress({
      status: 'processing',
      totalProcessed: 0,
      successCount: 0,
      failureCount: 0,
      successfulIds: [],
      failedIds: {},
      message: 'Processing...',
      processingTimeMs: 0
    });

    let response: BulkActionResponse;

    switch (action) {
      case 'start':
        response = await extractionService.bulkStart(ids);
        break;
      case 'stop':
        response = await extractionService.bulkStop(ids);
        break;
      case 'delete':
        response = await extractionService.bulkDelete(ids);
        break;
      case 'export':
        response = await extractionService.bulkExport(ids);
        break;
      default:
        throw new Error(`Unknown action: ${action}`);
    }

    setBulkProgress(response);
    clearSelection();
    loadExtractions();

    // Auto-close after 3 seconds if successful
    if (response.status === 'success') {
      setTimeout(() => {
        setBulkProgressOpen(false);
      }, 3000);
    }
  } catch (err: any) {
    setBulkProgress({
      status: 'failed',
      totalProcessed: ids.length,
      successCount: 0,
      failureCount: ids.length,
      successfulIds: [],
      failedIds: {},
      message: err.response?.data?.message || 'Bulk action failed',
      processingTimeMs: 0,
    });
  }
};

const handleConfirmBulkAction = async () => {
  if (pendingBulkAction) {
    await executeBulkAction(pendingBulkAction.action, pendingBulkAction.ids);
    setPendingBulkAction(null);
  }
  setConfirmDialogOpen(false);
};

const handleSelectAllClick = () => {
  if (isAllSelected(extractions)) {
    clearSelection();
  } else {
    selectAll(extractions);
  }
};

// Define bulk actions
const bulkActions = [
  BulkActionPresets.start(() => handleBulkAction('start')),
  BulkActionPresets.stop(() => handleBulkAction('stop')),
  BulkActionPresets.export(() => handleBulkAction('export')),
  BulkActionPresets.delete(() => handleBulkAction('delete')),
];
```

### Step 4: Update Table Header

```typescript
// In the TableHead section, ADD THIS AS THE FIRST COLUMN:
<TableCell padding="checkbox">
  <Checkbox
    indeterminate={isSomeSelected(extractions)}
    checked={extractions.length > 0 && isAllSelected(extractions)}
    onChange={handleSelectAllClick}
  />
</TableCell>

// Then all your existing sortable columns...
<TableCell>
  <TableSortLabel...>Name</TableSortLabel>
</TableCell>
// ... rest of columns
```

### Step 5: Update Table Rows

```typescript
// In the TableBody section, ADD THIS AS THE FIRST COLUMN in each row:
<TableCell padding="checkbox">
  <Checkbox
    checked={isSelected(extraction.id)}
    onChange={() => toggleSelection(extraction.id)}
  />
</TableCell>

// Then all your existing cells...
<TableCell>{extraction.name}</TableCell>
// ... rest of cells
```

### Step 6: Add BulkActionsToolbar

```typescript
// BEFORE the stats cards, add:
{/* Bulk Actions Toolbar */}
<BulkActionsToolbar
  selectedCount={selectedCount}
  actions={bulkActions}
  onClearSelection={clearSelection}
/>
```

### Step 7: Add Confirmation Dialog

```typescript
// Add this dialog AFTER the create dialog:
{/* Confirmation Dialog */}
<Dialog open={confirmDialogOpen} onClose={() => setConfirmDialogOpen(false)}>
  <DialogTitle>Confirm Bulk Action</DialogTitle>
  <DialogContent>
    <DialogContentText>
      Are you sure you want to {pendingBulkAction?.action} {selectedCount} extraction{selectedCount > 1 ? 's' : ''}?
      {pendingBulkAction?.action === 'delete' && ' This action cannot be undone.'}
    </DialogContentText>
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setConfirmDialogOpen(false)}>Cancel</Button>
    <Button onClick={handleConfirmBulkAction} variant="contained" color="error">
      Confirm
    </Button>
  </DialogActions>
</Dialog>
```

### Step 8: Add Bulk Progress Dialog

```typescript
// Add this dialog AFTER the confirmation dialog:
{/* Bulk Progress Dialog */}
<Dialog open={bulkProgressOpen} onClose={() => setBulkProgressOpen(false)} maxWidth="sm" fullWidth>
  <DialogTitle>Bulk Operation Progress</DialogTitle>
  <DialogContent>
    {bulkProgress && (
      <Box sx={{ pt: 2 }}>
        <Typography variant="body1" gutterBottom>
          {bulkProgress.message}
        </Typography>

        {bulkProgress.status === 'processing' ? (
          <LinearProgress sx={{ mt: 2 }} />
        ) : (
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Total Processed: {bulkProgress.totalProcessed}
            </Typography>
            <Typography variant="body2" color="success.main">
              Successful: {bulkProgress.successCount}
            </Typography>
            <Typography variant="body2" color="error.main">
              Failed: {bulkProgress.failureCount}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Processing Time: {bulkProgress.processingTimeMs}ms
            </Typography>

            {bulkProgress.failureCount > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Failed Items:
                </Typography>
                {Object.entries(bulkProgress.failedIds).map(([id, error]) => (
                  <Typography key={id} variant="caption" color="error.main" display="block">
                    {id}: {error}
                  </Typography>
                ))}
              </Box>
            )}
          </Box>
        )}
      </Box>
    )}
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setBulkProgressOpen(false)}>Close</Button>
  </DialogActions>
</Dialog>
```

### Step 9: Update Table Column Counts

```typescript
// Update all colSpan values to account for the new checkbox column
// For example:
<TableCell colSpan={6} align="center">  // was 6, now should be 7
  <CircularProgress />
</TableCell>
```

## Integration Steps for Migrations.tsx

Follow the exact same steps as Extractions.tsx with these differences:

### Bulk Actions Definition

```typescript
// For Migrations, include pause/resume actions:
const bulkActions = [
  BulkActionPresets.start(() => handleBulkAction('start')),
  BulkActionPresets.pause(() => handleBulkAction('pause')),
  BulkActionPresets.resume(() => handleBulkAction('resume')),
  BulkActionPresets.export(() => handleBulkAction('export')),
  BulkActionPresets.delete(() => handleBulkAction('delete')),
];
```

### Bulk Action Handler

```typescript
const executeBulkAction = async (action: string, ids: string[]) => {
  // ... setup code same as above ...

  switch (action) {
    case 'start':
      response = await migrationService.bulkStart(ids);
      break;
    case 'pause':
      response = await migrationService.bulkPause(ids);
      break;
    case 'resume':
      response = await migrationService.bulkResume(ids);
      break;
    case 'delete':
      response = await migrationService.bulkDelete(ids);
      break;
    case 'export':
      response = await migrationService.bulkExport(ids);
      break;
    default:
      throw new Error(`Unknown action: ${action}`);
  }

  // ... rest of code same as above ...
};
```

## Testing Checklist

### Manual Testing

- [ ] Select individual rows using checkboxes
- [ ] Click "Select All" checkbox - all visible rows should be selected
- [ ] Click "Select All" again - all rows should be deselected
- [ ] Select some rows, observe BulkActionsToolbar appears
- [ ] Click "Start" button - selected extractions should start
- [ ] Click "Delete" button - confirmation dialog should appear
- [ ] Confirm delete - bulk progress dialog should show results
- [ ] Verify successful operations show in success list
- [ ] Verify failed operations show with error messages
- [ ] Verify selection is cleared after bulk operation
- [ ] Verify table refreshes to show updated status
- [ ] Test with pagination - select rows, change page, verify selection preserved
- [ ] Test with filtering - filter results, select some, perform bulk action

### Automated Testing (Future)

```typescript
// Example test
describe('Bulk Operations', () => {
  it('should select all visible rows', () => {
    // Test implementation
  });

  it('should show bulk actions toolbar when items selected', () => {
    // Test implementation
  });

  it('should show confirmation for bulk delete', () => {
    // Test implementation
  });

  it('should display bulk operation results', () => {
    // Test implementation
  });
});
```

## Compatibility with Existing Features

### Works With Advanced Filtering (Workflow 7)
- Users can filter extractions by status, date, records, etc.
- Then select filtered results
- Perform bulk operations on filtered set
- Example: "Show all FAILED extractions from last week" → Select All → Bulk Delete

### Works With Search (Workflow 6)
- Users can search for specific extractions
- Select search results
- Perform bulk operations on search results
- Example: Search "customer_data" → Select matching → Bulk Start

### Works With Real-time Updates (Workflow 9)
- Bulk operation status changes update in real-time
- Selected items show live status transitions
- Progress dialog updates as operations complete

## Keyboard Shortcuts (Future Enhancement)

Suggested shortcuts to implement:
- `Ctrl+A` / `Cmd+A` - Select all visible rows
- `Delete` - Bulk delete selected items (with confirmation)
- `Escape` - Clear selection
- `Shift+Click` - Range select rows

## Accessibility Considerations

- All checkboxes have proper aria-labels
- Bulk action buttons have tooltips
- Progress dialog can be closed with Escape key
- Keyboard navigation works throughout
- Screen reader announces selected count

## Performance Notes

- Selection state uses `Set<string>` for O(1) lookups
- Only renders BulkActionsToolbar when items selected
- Bulk operations process sequentially (can optimize to parallel if needed)
- Consider adding pagination limits for bulk operations (e.g., max 100 at a time)

## Error Handling

- Invalid IDs are caught and reported in failedIds
- Network errors show in bulk progress dialog
- Partial success is handled gracefully (some succeed, some fail)
- User can retry failed items individually

## Next Steps

1. Apply these changes to `frontend/src/pages/Extractions.tsx`
2. Apply these changes to `frontend/src/pages/Migrations.tsx`
3. Test manually with various scenarios
4. Add E2E tests for bulk operations
5. Document bulk operations in user guide
6. Consider adding bulk operation limits (max 100 items)
7. Consider adding "Select All Pages" for bulk operations across pagination

---

**Status**: Ready for integration
**Estimated Time**: 30-45 minutes per page
**Risk**: Low (additive feature, doesn't break existing functionality)
**Rollback**: Simply remove the new code, original functionality remains intact
