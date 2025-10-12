# Advanced Filtering & Sorting - Visual Guide

## UI Components Overview

### 1. Quick Filters Bar
```
┌─────────────────────────────────────────────────────────────┐
│  ┌─────────┐  ┌─────────┐  ┌──────────────────┐  ┌────────┐│
│  │ Active  │  │ Failed  │  │ Completed Today  │  │ High   ││
│  │  INFO   │  │  ERROR  │  │     SUCCESS      │  │ Volume ││
│  └─────────┘  └─────────┘  └──────────────────┘  └────────┘│
└─────────────────────────────────────────────────────────────┘
```
- Click once to apply
- Click again to clear
- Color-coded by purpose
- Active filter highlighted

### 2. Advanced Filter Builder Button
```
┌────────────────────────┐  ┌─────────────────┐
│ [🔍] Filters (3)       │  │ Saved Views ▼   │
└────────────────────────┘  └─────────────────┘
```
- Badge shows active filter count
- Click to open filter dialog
- Dropdown for saved views

### 3. Filter Builder Dialog
```
┌────────────────────────────────────────────────────────────┐
│  Advanced Filters                                      [X] │
├────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐   │
│  │ [AND] [OR]                                    [❌]  │   │
│  │                                                      │   │
│  │  ┌─────────┐  ┌─────────────┐  ┌─────────┐  [❌]  │   │
│  │  │ Status  │  │   Equals    │  │ FAILED  │        │   │
│  │  └─────────┘  └─────────────┘  └─────────┘        │   │
│  │                                                      │   │
│  │  ┌─────────┐  ┌─────────────┐  ┌─────────┐  [❌]  │   │
│  │  │  Name   │  │  Contains   │  │  test   │        │   │
│  │  └─────────┘  └─────────────┘  └─────────┘        │   │
│  │                                                      │   │
│  │  [+ Add Condition]                                  │   │
│  └────────────────────────────────────────────────────┘   │
│                                                            │
│  [+ Add Filter Group]                                     │
│                                                            │
│  ┌─────────────┐  ┌────────┐  ┌─────────────────┐        │
│  │  Clear All  │  │ Cancel │  │  Apply Filters  │        │
│  └─────────────┘  └────────┘  └─────────────────┘        │
└────────────────────────────────────────────────────────────┘
```

### 4. Sortable Table Headers
```
┌──────────────────────────────────────────────────────────┐
│  Name ▲₁   SourceType   Status ▼₂   Records   Created   │
├──────────────────────────────────────────────────────────┤
│  Extract 1   JDBC        FAILED      1,000     Today    │
│  Extract 2   SAP         RUNNING     5,000     Today    │
│  Extract 3   FILE        COMPLETED   10,000    Yesterday│
└──────────────────────────────────────────────────────────┘
```
- ▲ = Ascending sort
- ▼ = Descending sort
- ₁₂₃ = Sort order (multi-column)
- Click = Single sort
- Shift+Click = Multi-column sort

### 5. Active Filters Display
```
┌─────────────────────────────────────────────────────────┐
│  Filters Applied:                                       │
│  ┌──────────────────────────┐  ┌────────────────────┐  │
│  │ Status = FAILED      [X] │  │ Name contains test │  │
│  └──────────────────────────┘  └────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```
- Shows active filters as chips
- Click X to remove individual filter
- Clear all button available

### 6. Saved View Dialog
```
┌─────────────────────────────────────────────────────────┐
│  Save Current View                                 [X]  │
├─────────────────────────────────────────────────────────┤
│  View Name:                                             │
│  ┌──────────────────────────────────────────────┐      │
│  │ My Failed Extractions                        │      │
│  └──────────────────────────────────────────────┘      │
│                                                         │
│  ☐ Share with team                                     │
│                                                         │
│  ┌─────────────────────────────────────────────┐       │
│  │  Filters: 2 group(s)                        │       │
│  │  Sort: 1 column(s)                          │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  ┌────────┐  ┌──────────────┐                         │
│  │ Cancel │  │  Save View   │                         │
│  └────────┘  └──────────────┘                         │
└─────────────────────────────────────────────────────────┘
```

## User Workflows

### Workflow 1: Apply Quick Filter
```
User Action                     System Response
───────────────────────────────────────────────────────────
1. Click "Failed" chip      →   Apply status=FAILED filter
2. Table updates            →   Show only failed extractions
3. Chip highlighted         →   Visual feedback active
4. Click chip again         →   Clear filter
5. Table resets             →   Show all extractions
```

### Workflow 2: Build Custom Filter
```
User Action                     System Response
───────────────────────────────────────────────────────────
1. Click "Filters" button   →   Open filter dialog
2. Select "Status" field    →   Show relevant operators
3. Select "Equals"          →   Show value input
4. Enter "FAILED"           →   Capture filter condition
5. Click "Add Condition"    →   Add new condition row
6. Select "Name" field      →   Show string operators
7. Select "Contains"        →   Show text input
8. Enter "test"             →   Capture second condition
9. Toggle to "OR"           →   Change group logic
10. Click "Apply Filters"   →   Close dialog, filter data
11. Button shows "(2)"      →   Badge indicates 2 active
```

### Workflow 3: Multi-Column Sort
```
User Action                     System Response
───────────────────────────────────────────────────────────
1. Click "Status" header    →   Sort by Status ▲
2. Table reorders           →   Data sorted ascending
3. Shift+Click "Name"       →   Add secondary sort
4. Header shows ▲₁ ▲₂       →   Visual sort indicators
5. Table reorders           →   Data sorted by both
6. Click "Status" again     →   Toggle to descending ▼₁
7. Sort chip appears        →   "Sorted by 2 columns"
8. Click sort chip          →   Clear all sorting
```

### Workflow 4: Save and Share View
```
User Action                     System Response
───────────────────────────────────────────────────────────
1. Apply filters            →   Filters active
2. Apply sort               →   Sort active
3. Click save icon          →   Open save dialog
4. Enter "My View"          →   Capture view name
5. Check "Share with team"  →   Mark as shared
6. Click "Save View"        →   POST to backend API
7. Dialog closes            →   View saved confirmation
8. Select from dropdown     →   View listed
9. Teammate opens dropdown  →   View visible (shared)
10. Teammate selects view   →   Filters/sort applied
```

### Workflow 5: Share via URL
```
User Action                     System Response
───────────────────────────────────────────────────────────
1. Apply filters            →   URL updates with params
2. Apply sort               →   URL updates with sort
3. Copy URL                 →   URL contains state
4. Send to teammate         →   Share link
5. Teammate opens URL       →   Parse query params
6. Page loads               →   Filters/sort restored
7. Data displayed           →   Same view as sender
```

## Filter Logic Examples

### Example 1: Simple AND Filter
```
Group 1 (AND):
  - Status equals "FAILED"
  - Records > 1000

SQL:
WHERE status = 'FAILED' AND records_extracted > 1000
```

### Example 2: Simple OR Filter
```
Group 1 (OR):
  - Status equals "RUNNING"
  - Status equals "PENDING"

SQL:
WHERE status = 'RUNNING' OR status = 'PENDING'
```

### Example 3: Complex Multi-Group
```
Group 1 (AND):
  - Status equals "FAILED"
  - Records > 5000

Group 2 (OR):
  - Name contains "critical"
  - Name starts with "PROD"

Combined (Groups are ORed):
(status = 'FAILED' AND records > 5000)
OR
(name LIKE '%critical%' OR name LIKE 'PROD%')

SQL:
WHERE (status = 'FAILED' AND records_extracted > 5000)
   OR (name LIKE '%critical%' OR name LIKE 'PROD%')
```

## URL Structure

### Filter URL Format
```
/extractions?filters=%5B%7B...%7D%5D&sort=%5B%7B...%7D%5D

Decoded:
/extractions?filters=[{
  "id": "1",
  "logic": "AND",
  "conditions": [{
    "id": "1",
    "field": "status",
    "operator": "equals",
    "value": "FAILED"
  }]
}]&sort=[{
  "field": "createdAt",
  "direction": "desc"
}]
```

## Visual States

### Loading State
```
┌────────────────────────────────────┐
│                                    │
│         ⏳ Loading filters...      │
│                                    │
└────────────────────────────────────┘
```

### Empty State
```
┌────────────────────────────────────┐
│   📭 No filters applied            │
│   Click "Filters" to get started   │
└────────────────────────────────────┘
```

### Error State
```
┌────────────────────────────────────┐
│   ⚠️  Failed to load saved views   │
│   [Retry]                          │
└────────────────────────────────────┘
```

### Success State
```
┌────────────────────────────────────┐
│   ✓ View saved successfully        │
└────────────────────────────────────┘
```

## Color Coding

### Quick Filter Colors
- **Info (Blue)**: Active status filters
- **Success (Green)**: Completed status filters
- **Error (Red)**: Failed status filters
- **Warning (Orange)**: Conditional/threshold filters

### Status Indicators
- **Primary (Blue)**: Applied filters
- **Default (Gray)**: Available but not active
- **Error (Red)**: Invalid or error state

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl/Cmd + F` | Open filter builder |
| `Ctrl/Cmd + K` | Focus saved views dropdown |
| `Shift + Click` | Add multi-column sort |
| `Escape` | Close filter dialog |
| `Enter` | Apply filters (in dialog) |
| `Tab` | Navigate filter conditions |

## Responsive Behavior

### Desktop (> 1200px)
- Full filter builder dialog
- All quick filters visible
- Multi-column table with all columns

### Tablet (768px - 1200px)
- Scrollable quick filters
- Responsive filter dialog
- Hide non-essential columns

### Mobile (< 768px)
- Collapsible filter section
- Vertical quick filter chips
- Single column view with expandable rows
- Bottom sheet for filter builder

## Best Practices

### For Users
1. Use quick filters for common scenarios
2. Save complex filters as views
3. Share views with team for consistency
4. Use URL sharing for one-off scenarios
5. Limit multi-column sort to 3 columns

### For Developers
1. Index filterable columns in database
2. Validate filter inputs on backend
3. Limit filter group complexity (10 max)
4. Cache saved views
5. Log slow filter queries for optimization
