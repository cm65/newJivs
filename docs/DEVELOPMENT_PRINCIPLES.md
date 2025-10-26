# JiVS Platform - Development Principles

## 🚨 CRITICAL: Feature Isolation Principle

**ALWAYS follow this workflow when adding or modifying features:**

### Step 1: Establish Baseline ✅
```bash
# Before starting ANY work, document what's working
1. Run full test suite
2. Document passing tests/endpoints
3. Save baseline metrics
4. Commit current state to git
```

### Step 2: Plan Feature Scope 📋
```
BEFORE writing code, answer:
- What files will I CREATE? (new feature files)
- What files will I MODIFY? (only if absolutely necessary)
- What tests will I add?
- What existing features might be affected?
```

### Step 3: Implement in Isolation 🔒
```
DO:
✅ Create NEW files for new features
✅ CHECK EXISTING SIMILAR FILES for patterns (e.g., Extractions.tsx for API calls)
✅ FOLLOW ESTABLISHED CONVENTIONS (e.g., use /documents not /api/v1/documents)
✅ VERIFY CONFIGURATION FILES (e.g., check apiClient.ts baseURL)
✅ Add NEW endpoints for new functionality
✅ Add NEW database migrations
✅ Add NEW tests for new features
✅ Test the NEW feature in isolation

DO NOT:
❌ Modify working files "just to clean up"
❌ "Fix" unrelated code while implementing
❌ Add getters/setters to existing classes unnecessarily
❌ Refactor existing features unless explicitly required
❌ Change enum values in production code
❌ Modify database columns that other features depend on
❌ Duplicate configuration that already exists elsewhere (e.g., baseURL)
❌ Ignore existing patterns in similar components
```

### Step 4: Test New Feature Only 🧪
```bash
# Test ONLY the new feature first
1. Test new endpoints in isolation
2. Verify new functionality works
3. Check database changes applied correctly
4. Verify no compilation errors
```

### Step 5: Regression Test 🔄
```bash
# AFTER new feature works, verify existing features
1. Run existing test suite
2. Test previously working endpoints
3. Compare with baseline from Step 1
4. If anything broke, FIX IT before proceeding
```

### Step 6: Document Changes 📝
```
Record:
- Files CREATED (new)
- Files MODIFIED (with reason)
- Tests ADDED
- Existing features VERIFIED still working
```

## Real Example: Document Archiving Feature

### ✅ Correct Approach:
```
CREATED (New files only):
- DocumentController.java
- Document.java
- DocumentService.java
- DocumentRepository.java
- DocumentDTO.java
- Documents.tsx (frontend page)
- V101__Enhance_document_archiving.sql

MODIFIED (Only to fix issues in MY new code):
- Document.java - Fixed column mapping created_date → created_at
- Documents.tsx - Fixed duplicate /api/v1 prefix (see lesson below)

TESTED:
1. Document endpoints in isolation
2. Verified existing extraction/migration/compliance endpoints still work
```

### ❌ Lesson Learned: Not Checking Existing Patterns

**What Went Wrong:**
When creating `Documents.tsx`, I wrote API calls like:
```typescript
apiClient.get('/api/v1/documents/statistics')  // WRONG - duplicate prefix
```

**Root Cause:**
- Failed to check `apiClient.ts` which has `baseURL: '/api/v1'`
- Failed to look at `Extractions.tsx` which uses `apiClient.get('/extractions')`
- Ignored established convention in existing code

**Correct Implementation:**
```typescript
// Step 1: CHECK apiClient.ts
// Found: baseURL: 'http://localhost:8080/api/v1'

// Step 2: CHECK similar page (Extractions.tsx)
// Found: apiClient.get('/extractions')  ← No /api/v1 prefix!

// Step 3: FOLLOW the pattern
apiClient.get('/documents/statistics')  // CORRECT
```

**Impact:**
- All 8 API endpoints returned 404 errors
- Documents page showed "An unexpected error occurred"
- Wasted time debugging self-created issue

**Prevention:**
✅ ALWAYS check existing similar files before writing new code
✅ Look for configuration files that might affect your code
✅ Follow established patterns - consistency is key
✅ Test early to catch configuration errors quickly

### ❌ Wrong Approach (What NOT to do):
```
MODIFIED (Unnecessary changes):
- ExtractionConfig.java - Added getters (not needed, was working)
- BulkActionRequest.java - Added getters (not needed, was working)
- BulkActionResponse.java - Added builder (not needed, was working)
- RefreshTokenRequest.java - Added getters (not needed, was working)

RESULT:
- Wasted time on unnecessary changes
- Risk of breaking working features
- Distracted from actual goal (document archiving)
```

## Golden Rules 🏆

1. **"If it ain't broke, don't fix it"**
   - Working code is sacred
   - Don't modify unless absolutely necessary

2. **"Feature isolation is safety"**
   - New features should be self-contained
   - Minimize touching existing code

3. **"Test what you touch"**
   - If you modify a file, test that feature
   - Always regression test existing features

4. **"Plan before code"**
   - Write down what files you'll create/modify
   - Get approval if modifying critical files

5. **"Baseline is your friend"**
   - Always know what was working before you started
   - Document the before state

6. **"Check existing patterns FIRST"** ⭐ NEW
   - Before creating new code, look at similar existing code
   - Follow established conventions and patterns
   - Example: Check how Extractions.tsx makes API calls before writing Documents.tsx
   - Check configuration files (apiClient.ts) before duplicating settings
   - Consistency prevents bugs

## Pre-Commit Checklist ☑️

Before committing ANY code:

- [ ] Did I only modify files necessary for this feature?
- [ ] Did I test the new feature in isolation?
- [ ] Did I verify existing features still work?
- [ ] Did I revert any unnecessary changes?
- [ ] Did I document what I changed and why?
- [ ] Would my changes break production if deployed?

## When Modifying Existing Code IS Necessary

Sometimes you MUST modify existing code. When you do:

1. **Document the reason** - Why is this modification necessary?
2. **Test extensively** - Test the modified feature thoroughly
3. **Communicate** - Inform team about changes to shared code
4. **Plan rollback** - Know how to revert if things break

Examples of valid reasons:
- ✅ Bug fix in existing feature
- ✅ Security vulnerability
- ✅ Database schema requires backward-compatible change
- ✅ API contract needs versioning
- ✅ Performance optimization (after profiling)

Examples of invalid reasons:
- ❌ "Code looks messy"
- ❌ "I prefer this pattern"
- ❌ "Adding getters for consistency"
- ❌ "Fixing Lombok compilation issues" (when it's not broken)

## Emergency Rollback Procedure 🚨

If you break existing features:

```bash
# 1. Immediately stop
git status

# 2. Identify what you changed
git diff

# 3. Revert unnecessary changes
git checkout -- <file-that-shouldnt-have-been-modified>

# 4. Test again
# Run test suite

# 5. If still broken, revert everything
git reset --hard HEAD

# 6. Start over with proper planning
```

## Metrics to Track

For each feature implementation:
- **Files created**: X new files
- **Files modified**: Y existing files (justify each)
- **Tests added**: Z new tests
- **Existing tests passing**: Before: A%, After: A% (should be same)
- **Time spent**: X hours (compare to estimate)

## Remember:

> **"The best code is code that doesn't break existing functionality."**
>
> **"New features should be additive, not disruptive."**
>
> **"When in doubt, test more, change less."**

---

**Created**: January 2025
**Last Updated**: January 2025
**Status**: MANDATORY - Follow for ALL development work
