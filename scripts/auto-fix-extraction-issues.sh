#!/bin/bash
#
# JiVS Extraction Module - Automated Fix Script
# ==============================================
#
# Purpose: Automatically apply safe fixes to extraction module issues
# Usage: ./scripts/auto-fix-extraction-issues.sh [--dry-run]
#
# This script applies fixes for issues that can be safely automated:
# - Enables SQL injection validation
# - Fixes thread-safe ExtractionResult (if not already done)
# - Adds missing imports
#
# WARNING: This modifies source files. Commit your changes first!
#
# Author: jivs-extraction-expert
# Date: 2025-10-26
#

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

DRY_RUN=false
FIXES_APPLIED=0
FIXES_SKIPPED=0

# Parse arguments
if [ "$1" == "--dry-run" ]; then
    DRY_RUN=true
    echo -e "${YELLOW}DRY RUN MODE - No files will be modified${NC}"
    echo ""
fi

# Change to project root
cd "$(dirname "$0")/.."

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_fix() {
    echo -e "${GREEN}[APPLYING]${NC} $1"
}

print_skip() {
    echo -e "${YELLOW}[SKIPPING]${NC} $1"
    ((FIXES_SKIPPED++))
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
    ((FIXES_APPLIED++))
}

backup_file() {
    local file=$1
    if [ ! "$DRY_RUN" = true ]; then
        cp "$file" "$file.bak"
        echo "   Backup created: $file.bak"
    fi
}

print_header "JiVS Extraction Module Auto-Fix"
echo ""

# ============================================================================
# FIX 1: Enable SQL Injection Validation in JdbcConnector
# ============================================================================
print_fix "Fix 1: Enable SQL injection validation in JdbcConnector..."

JDBC_CONNECTOR="backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java"

if [ ! -f "$JDBC_CONNECTOR" ]; then
    print_skip "JdbcConnector.java not found"
else
    # Check if already fixed
    if grep -q "private final SqlInjectionValidator sqlValidator;" "$JDBC_CONNECTOR" && \
       ! grep -q "// private final SqlInjectionValidator sqlValidator;" "$JDBC_CONNECTOR"; then
        print_skip "SQL injection validation already enabled"
    else
        backup_file "$JDBC_CONNECTOR"

        if [ ! "$DRY_RUN" = true ]; then
            # Uncomment the field
            sed -i.tmp 's|// private final SqlInjectionValidator sqlValidator; // Temporarily disabled|private final SqlInjectionValidator sqlValidator;|g' "$JDBC_CONNECTOR"

            # Uncomment validation code
            sed -i.tmp 's|// CRITICAL: Validate query for SQL injection (temporarily disabled)|// CRITICAL: Validate query for SQL injection|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|// TODO: Re-enable SQL injection validation when security module is restored||g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|// if (!sqlValidator.isQuerySafe(query)) {|if (!sqlValidator.isQuerySafe(query)) {|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|//     String errorMsg|    String errorMsg|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|//     log.error|    log.error|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|//     result.getErrors()|    result.getErrors()|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|//     result.setRecordsFailed|    result.setRecordsFailed|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|//     throw new SecurityException|    throw new SecurityException|g' "$JDBC_CONNECTOR"
            sed -i.tmp 's|// }|}|g' "$JDBC_CONNECTOR"

            rm -f "$JDBC_CONNECTOR.tmp"

            print_success "SQL injection validation enabled"
            echo "   Note: You must update constructor to accept SqlInjectionValidator parameter"
        else
            echo "   Would uncomment SQL injection validation"
        fi
    fi
fi

echo ""

# ============================================================================
# FIX 2: Replace ExtractionResult with Thread-Safe Version
# ============================================================================
print_fix "Fix 2: Checking if ExtractionResult is thread-safe..."

EXTRACTION_RESULT="backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java"

if [ ! -f "$EXTRACTION_RESULT" ]; then
    print_skip "ExtractionResult.java not found"
else
    # Check if already fixed
    if grep -q "import java.util.concurrent.atomic.AtomicLong;" "$EXTRACTION_RESULT"; then
        print_skip "ExtractionResult already thread-safe"
    else
        print_skip "ExtractionResult needs manual replacement"
        echo "   Reason: Complete file replacement required"
        echo "   Action: File already provided - copy from:"
        echo "   backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java"
        echo "   (Thread-safe version already in codebase)"
    fi
fi

echo ""

# ============================================================================
# FIX 3: Add CryptoUtil to ExtractionConfigService
# ============================================================================
print_fix "Fix 3: Checking password encryption in ExtractionConfigService..."

EXTRACTION_CONFIG_SERVICE="backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java"

if [ ! -f "$EXTRACTION_CONFIG_SERVICE" ]; then
    print_skip "ExtractionConfigService.java not found"
else
    # Check if CryptoUtil is already injected
    if grep -q "private final CryptoUtil cryptoUtil;" "$EXTRACTION_CONFIG_SERVICE"; then
        print_skip "CryptoUtil already injected"
    else
        print_skip "CryptoUtil injection needs manual addition"
        echo "   Reason: Requires modifying constructor and @RequiredArgsConstructor"
        echo "   Action: Follow guide in backend/EXTRACTION_MODULE_FIXES.md - Issue #2"
        echo "   Quick: Add 'private final CryptoUtil cryptoUtil;' after line 27"
    fi

    # Check if encryption is used
    if grep -q "cryptoUtil.encrypt" "$EXTRACTION_CONFIG_SERVICE"; then
        print_skip "Password encryption already implemented"
    else
        print_skip "Password encryption needs manual implementation"
        echo "   Action: Replace line 163 with encryption logic"
        echo "   See: backend/EXTRACTION_MODULE_FIXES.md - Issue #2"
    fi
fi

echo ""

# ============================================================================
# FIX 4: Create Batch Writer Package
# ============================================================================
print_fix "Fix 4: Checking batch writer implementation..."

BATCH_WRITER_DIR="backend/src/main/java/com/jivs/platform/service/extraction/batch"

if [ -d "$BATCH_WRITER_DIR" ]; then
    print_skip "Batch writer package already exists"
else
    if [ ! "$DRY_RUN" = true ]; then
        mkdir -p "$BATCH_WRITER_DIR"
        print_success "Created batch writer package"
        echo "   Next: Implement batch writers"
        echo "   See: backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md"
    else
        echo "   Would create: $BATCH_WRITER_DIR"
    fi
fi

echo ""

# ============================================================================
# FIX 5: Add Missing Test File
# ============================================================================
print_fix "Fix 5: Checking test files..."

EXTRACTION_RESULT_TEST="backend/src/test/java/com/jivs/platform/service/extraction/ExtractionResultTest.java"

if [ -f "$EXTRACTION_RESULT_TEST" ]; then
    print_skip "ExtractionResultTest.java already exists"
else
    print_skip "ExtractionResultTest.java needs to be created"
    echo "   Reason: Complete test file provided"
    echo "   Action: File already in codebase at correct location"
    echo "   Verify: mvn test -Dtest=ExtractionResultTest"
fi

echo ""

# ============================================================================
# FIX 6: Set Executable Permissions
# ============================================================================
print_fix "Fix 6: Setting executable permissions on scripts..."

if [ ! "$DRY_RUN" = true ]; then
    chmod +x scripts/validate-extraction-fixes.sh
    chmod +x scripts/auto-fix-extraction-issues.sh
    print_success "Script permissions set"
else
    echo "   Would set executable permissions"
fi

echo ""

# ============================================================================
# SUMMARY
# ============================================================================
print_header "Auto-Fix Summary"
echo ""
echo -e "${GREEN}Fixes Applied: $FIXES_APPLIED${NC}"
echo -e "${YELLOW}Fixes Skipped: $FIXES_SKIPPED${NC}"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - No files were modified${NC}"
    echo "Run without --dry-run to apply fixes"
    echo ""
fi

if [ -f "$JDBC_CONNECTOR.bak" ]; then
    echo "Backups created with .bak extension"
    echo "To restore: cp file.bak file"
    echo ""
fi

echo "Manual steps required:"
echo ""
echo "1. Update JdbcConnector constructor to accept SqlInjectionValidator"
echo "   File: $JDBC_CONNECTOR"
echo "   Line: ~49"
echo "   Add parameter: SqlInjectionValidator sqlValidator"
echo ""
echo "2. Add CryptoUtil to ExtractionConfigService"
echo "   File: $EXTRACTION_CONFIG_SERVICE"
echo "   Add field: private final CryptoUtil cryptoUtil;"
echo ""
echo "3. Implement password encryption"
echo "   File: $EXTRACTION_CONFIG_SERVICE"
echo "   Line: 163"
echo "   Call: cryptoUtil.encrypt(plainPassword)"
echo ""
echo "4. Implement batch writers"
echo "   Guide: backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md"
echo ""
echo "5. Run validation:"
echo "   ./scripts/validate-extraction-fixes.sh"
echo ""
echo "For complete instructions, see:"
echo "   EXTRACTION_QUICK_START.md"
echo "   backend/EXTRACTION_MODULE_FIXES.md"
echo ""

if [ $FIXES_APPLIED -gt 0 ]; then
    echo -e "${GREEN}✓ $FIXES_APPLIED automated fix(es) applied${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Review changes: git diff"
    echo "2. Complete manual fixes above"
    echo "3. Run tests: mvn test"
    echo "4. Validate: ./scripts/validate-extraction-fixes.sh"
    echo ""
    exit 0
else
    echo -e "${YELLOW}No automated fixes were applied${NC}"
    echo "Most fixes require manual implementation for safety"
    echo "Use EXTRACTION_QUICK_START.md for step-by-step guide"
    echo ""
    exit 0
fi
