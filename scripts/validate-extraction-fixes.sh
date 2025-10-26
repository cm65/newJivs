#!/bin/bash
#
# JiVS Extraction Module - Pre-Commit Validation Script
# =====================================================
#
# Purpose: Validate that critical extraction module fixes are in place
# Usage: ./scripts/validate-extraction-fixes.sh
# Exit codes: 0 = all checks pass, 1 = validation failures
#
# This script checks for common security and functionality issues
# identified in the extraction module audit.
#
# Author: jivs-extraction-expert
# Date: 2025-10-26
#

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
CHECKS_PASSED=0
CHECKS_FAILED=0
CHECKS_WARNING=0

# Print functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_check() {
    echo -e "${BLUE}[CHECK]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((CHECKS_PASSED++))
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((CHECKS_FAILED++))
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    ((CHECKS_WARNING++))
}

# Change to project root
cd "$(dirname "$0")/.."

print_header "JiVS Extraction Module Validation"
echo ""

# ============================================================================
# CHECK 1: SQL Injection Validation Enabled
# ============================================================================
print_check "Verifying SQL injection validation is enabled..."

JDBC_CONNECTOR="backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java"

if [ ! -f "$JDBC_CONNECTOR" ]; then
    print_fail "JdbcConnector.java not found"
else
    # Check if validation code is uncommented
    if grep -q "if (!sqlValidator.isQuerySafe(query))" "$JDBC_CONNECTOR" && \
       ! grep -q "// if (!sqlValidator.isQuerySafe(query))" "$JDBC_CONNECTOR"; then
        print_pass "SQL injection validation is ENABLED"
    else
        print_fail "SQL injection validation is DISABLED or commented out"
        echo "       Fix: Uncomment validation in JdbcConnector.java lines 90-98"
        echo "       See: backend/EXTRACTION_MODULE_FIXES.md - Issue #1"
    fi

    # Check if SqlInjectionValidator field is enabled
    if grep -q "private final SqlInjectionValidator sqlValidator;" "$JDBC_CONNECTOR" && \
       ! grep -q "// private final SqlInjectionValidator sqlValidator;" "$JDBC_CONNECTOR"; then
        print_pass "SqlInjectionValidator field is declared"
    else
        print_fail "SqlInjectionValidator field is commented out"
        echo "       Fix: Uncomment field declaration at line 46"
    fi

    # Check constructor has validator parameter
    if grep -q "String dbType, SqlInjectionValidator sqlValidator" "$JDBC_CONNECTOR"; then
        print_pass "Constructor accepts SqlInjectionValidator"
    else
        print_fail "Constructor missing SqlInjectionValidator parameter"
        echo "       Fix: Add SqlInjectionValidator to constructor"
    fi
fi

echo ""

# ============================================================================
# CHECK 2: Password Encryption
# ============================================================================
print_check "Verifying password encryption is implemented..."

EXTRACTION_CONFIG_SERVICE="backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java"

if [ ! -f "$EXTRACTION_CONFIG_SERVICE" ]; then
    print_fail "ExtractionConfigService.java not found"
else
    # Check for CryptoUtil injection
    if grep -q "private final CryptoUtil cryptoUtil;" "$EXTRACTION_CONFIG_SERVICE"; then
        print_pass "CryptoUtil is injected"
    else
        print_fail "CryptoUtil is NOT injected"
        echo "       Fix: Add CryptoUtil to ExtractionConfigService"
        echo "       See: backend/EXTRACTION_MODULE_FIXES.md - Issue #2"
    fi

    # Check for encryption usage
    if grep -q "cryptoUtil.encrypt" "$EXTRACTION_CONFIG_SERVICE"; then
        print_pass "Password encryption is used"
    else
        print_fail "Password encryption is NOT used"
        echo "       Fix: Call cryptoUtil.encrypt() before setPasswordEncrypted()"
    fi

    # Check for TODO comment (should be removed)
    if grep -q "TODO: Encrypt properly" "$EXTRACTION_CONFIG_SERVICE"; then
        print_fail "TODO comment still present - encryption not implemented"
        echo "       Fix: Remove TODO and implement encryption"
    else
        print_pass "No encryption TODO comments found"
    fi
fi

# Check for migration script
MIGRATION_SCRIPT="backend/src/main/resources/db/migration/V111__Encrypt_existing_passwords.sql"
if [ -f "$MIGRATION_SCRIPT" ]; then
    print_pass "Password encryption migration script exists"
else
    print_warn "Password encryption migration script not found"
    echo "       Note: Script should be at V111__Encrypt_existing_passwords.sql"
fi

echo ""

# ============================================================================
# CHECK 3: Thread-Safe ExtractionResult
# ============================================================================
print_check "Verifying ExtractionResult is thread-safe..."

EXTRACTION_RESULT="backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java"

if [ ! -f "$EXTRACTION_RESULT" ]; then
    print_fail "ExtractionResult.java not found"
else
    # Check for AtomicLong usage
    if grep -q "import java.util.concurrent.atomic.AtomicLong;" "$EXTRACTION_RESULT" && \
       grep -q "private final AtomicLong recordsExtracted" "$EXTRACTION_RESULT"; then
        print_pass "ExtractionResult uses AtomicLong for counters"
    else
        print_fail "ExtractionResult NOT using AtomicLong"
        echo "       Fix: Replace Long fields with AtomicLong"
        echo "       See: backend/EXTRACTION_MODULE_FIXES.md - Issue #5"
    fi

    # Check for CopyOnWriteArrayList usage
    if grep -q "import java.util.concurrent.CopyOnWriteArrayList;" "$EXTRACTION_RESULT" && \
       grep -q "CopyOnWriteArrayList" "$EXTRACTION_RESULT"; then
        print_pass "ExtractionResult uses CopyOnWriteArrayList for errors"
    else
        print_fail "ExtractionResult NOT using CopyOnWriteArrayList"
        echo "       Fix: Replace ArrayList with CopyOnWriteArrayList"
    fi
fi

echo ""

# ============================================================================
# CHECK 4: Batch Processing Implementation
# ============================================================================
print_check "Verifying batch processing is implemented..."

JDBC_CONNECTOR="backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java"

if [ ! -f "$JDBC_CONNECTOR" ]; then
    print_fail "JdbcConnector.java not found"
else
    # Check for TODO in processBatch method
    if grep -A 5 "private void processBatch" "$JDBC_CONNECTOR" | grep -q "TODO: Implement actual batch processing"; then
        print_fail "Batch processing is still a TODO placeholder"
        echo "       CRITICAL: This means extracted data is DISCARDED"
        echo "       Fix: Implement batch writers (Parquet/CSV/JSON)"
        echo "       See: backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md"
    else
        # Check if batch writer implementation exists
        if grep -q "BatchWriter" "$JDBC_CONNECTOR"; then
            print_pass "Batch processing implementation found"
        else
            print_warn "Batch processing TODO removed but implementation unclear"
        fi
    fi
fi

# Check for batch writer classes
BATCH_WRITER_DIR="backend/src/main/java/com/jivs/platform/service/extraction/batch"
if [ -d "$BATCH_WRITER_DIR" ]; then
    print_pass "Batch writer package exists"

    # Check for specific writers
    if [ -f "$BATCH_WRITER_DIR/ParquetBatchWriter.java" ]; then
        print_pass "ParquetBatchWriter implemented"
    else
        print_warn "ParquetBatchWriter not found"
    fi

    if [ -f "$BATCH_WRITER_DIR/CsvBatchWriter.java" ]; then
        print_pass "CsvBatchWriter implemented"
    else
        print_warn "CsvBatchWriter not found"
    fi
else
    print_fail "Batch writer package does not exist"
    echo "       Fix: Create batch writer implementations"
fi

echo ""

# ============================================================================
# CHECK 5: Test Coverage
# ============================================================================
print_check "Verifying test coverage..."

# Check for ExtractionResultTest
EXTRACTION_RESULT_TEST="backend/src/test/java/com/jivs/platform/service/extraction/ExtractionResultTest.java"
if [ -f "$EXTRACTION_RESULT_TEST" ]; then
    print_pass "ExtractionResultTest exists"

    # Check for concurrency tests
    if grep -q "shouldHandleConcurrentRecordIncrement" "$EXTRACTION_RESULT_TEST"; then
        print_pass "Concurrency tests present"
    else
        print_warn "Concurrency tests not found in ExtractionResultTest"
    fi
else
    print_fail "ExtractionResultTest not found"
    echo "       Fix: Create ExtractionResultTest.java"
fi

# Check for SqlInjectionValidatorTest
SQL_INJECTION_TEST="backend/src/test/java/com/jivs/platform/security/SqlInjectionValidatorTest.java"
if [ -f "$SQL_INJECTION_TEST" ]; then
    print_pass "SqlInjectionValidatorTest exists"
else
    print_warn "SqlInjectionValidatorTest not found"
    echo "       Recommendation: Create tests for SQL injection validation"
fi

# Check if ExtractionContractTest compiles
EXTRACTION_CONTRACT_TEST="backend/src/test/java/com/jivs/platform/contract/ExtractionContractTest.java"
if [ -f "$EXTRACTION_CONTRACT_TEST" ]; then
    # Check for broken imports
    if grep -q "import com.jivs.platform.domain.extraction.Extraction;" "$EXTRACTION_CONTRACT_TEST"; then
        print_fail "ExtractionContractTest uses non-existent Extraction entity"
        echo "       Fix: Rewrite test to use ExtractionConfig + ExtractionJob"
        echo "       See: backend/EXTRACTION_MODULE_TEST_SUITE.md"
    else
        print_pass "ExtractionContractTest appears to use correct entities"
    fi
else
    print_warn "ExtractionContractTest not found"
fi

echo ""

# ============================================================================
# CHECK 6: Resource Leak Prevention
# ============================================================================
print_check "Verifying resource leak fixes..."

if [ -f "$JDBC_CONNECTOR" ]; then
    # Check for connection close in finally block
    if grep -A 10 "} finally {" "$JDBC_CONNECTOR" | grep -q "connection.close()"; then
        print_pass "Connection close found in finally block"
    else
        print_fail "Connection NOT closed in finally block"
        echo "       Fix: Add connection.close() in finally block"
        echo "       See: backend/EXTRACTION_MODULE_FIXES.md - Issue #6"
    fi
fi

echo ""

# ============================================================================
# CHECK 7: Configuration & Security
# ============================================================================
print_check "Verifying security configurations..."

# Check application.yml for encryption key
APP_YML="backend/src/main/resources/application.yml"
if [ -f "$APP_YML" ]; then
    if grep -q "encryption" "$APP_YML" || grep -q "crypto" "$APP_YML"; then
        print_pass "Encryption configuration found in application.yml"
    else
        print_warn "No encryption configuration in application.yml"
        echo "       Recommendation: Add jivs.security.encryption-key property"
    fi
else
    print_warn "application.yml not found"
fi

echo ""

# ============================================================================
# SUMMARY
# ============================================================================
print_header "Validation Summary"
echo ""
echo -e "${GREEN}Passed:  $CHECKS_PASSED${NC}"
echo -e "${YELLOW}Warnings: $CHECKS_WARNING${NC}"
echo -e "${RED}Failed:  $CHECKS_FAILED${NC}"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All critical checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Run unit tests: mvn test"
    echo "2. Run integration tests: mvn verify -P integration-tests"
    echo "3. Review deployment runbook: backend/EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md"
    echo ""
    exit 0
else
    echo -e "${RED}✗ $CHECKS_FAILED critical check(s) failed${NC}"
    echo ""
    echo "IMPORTANT: Fix all failures before deploying to production!"
    echo ""
    echo "Quick fixes available:"
    echo "1. SQL injection: backend/EXTRACTION_MODULE_FIXES.md - Issue #1"
    echo "2. Password encryption: backend/EXTRACTION_MODULE_FIXES.md - Issue #2"
    echo "3. Thread safety: backend/EXTRACTION_MODULE_FIXES.md - Issue #5"
    echo "4. Batch processing: backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md"
    echo ""
    echo "Or use quick start: EXTRACTION_QUICK_START.md"
    echo ""
    exit 1
fi
