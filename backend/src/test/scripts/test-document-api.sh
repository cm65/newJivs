#!/bin/bash

###############################################################################
# Document Archiving API Test Script
# Tests all document API endpoints with curl
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_PREFIX="/api/v1/documents"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-password}"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to print colored output
print_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
  echo -e "${RED}✗ $1${NC}"
}

print_info() {
  echo -e "${YELLOW}ℹ $1${NC}"
}

# Function to run test
run_test() {
  local test_name="$1"
  local expected_status="$2"
  local actual_status="$3"

  TOTAL_TESTS=$((TOTAL_TESTS + 1))

  if [ "$actual_status" -eq "$expected_status" ]; then
    print_success "$test_name (Status: $actual_status)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
  else
    print_error "$test_name (Expected: $expected_status, Got: $actual_status)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
  fi
}

# Function to authenticate
authenticate() {
  print_info "Authenticating..."

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

  status=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [ "$status" -eq 200 ]; then
    TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    print_success "Authentication successful"
    return 0
  else
    print_error "Authentication failed (Status: $status)"
    exit 1
  fi
}

# Test 1: Upload Document
test_upload_document() {
  print_info "Test 1: Upload Document"

  # Create temporary test file
  echo "This is a test document for JiVS archiving system" > /tmp/test-doc.txt

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}${API_PREFIX}/upload" \
    -H "Authorization: Bearer ${TOKEN}" \
    -F "file=@/tmp/test-doc.txt" \
    -F "title=Test Document" \
    -F "description=Test document for API testing" \
    -F "tags=test,api,automation")

  status=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  run_test "Upload Document" 200 "$status"

  if [ "$status" -eq 200 ]; then
    DOCUMENT_ID=$(echo "$body" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    print_info "Document ID: $DOCUMENT_ID"
  fi

  # Cleanup
  rm -f /tmp/test-doc.txt
}

# Test 2: Get Document by ID
test_get_document() {
  print_info "Test 2: Get Document by ID"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Get Document by ID" 200 "$status"
}

# Test 3: Search Documents
test_search_documents() {
  print_info "Test 3: Search Documents"

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}${API_PREFIX}/search" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"query":"test","from":0,"size":10}')

  status=$(echo "$response" | tail -n1)
  run_test "Search Documents" 200 "$status"
}

# Test 4: Get All Documents
test_get_all_documents() {
  print_info "Test 4: Get All Documents"

  response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}${API_PREFIX}?page=0&size=20" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Get All Documents" 200 "$status"
}

# Test 5: Archive Document
test_archive_document() {
  print_info "Test 5: Archive Document"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}/archive?compress=true&storageTier=WARM" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  run_test "Archive Document" 200 "$status"

  if [ "$status" -eq 200 ]; then
    ARCHIVE_ID=$(echo "$body" | grep -o '"archiveId":"[^"]*' | cut -d'"' -f4)
    print_info "Archive ID: $ARCHIVE_ID"
  fi
}

# Test 6: Restore Document
test_restore_document() {
  print_info "Test 6: Restore Document"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}/restore" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Restore Document" 200 "$status"
}

# Test 7: Get Document Content
test_get_document_content() {
  print_info "Test 7: Get Document Content"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}/content" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Get Document Content" 200 "$status"
}

# Test 8: Update Document
test_update_document() {
  print_info "Test 8: Update Document"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X PUT \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"title":"Updated Test Document","description":"Updated description","tags":["updated","test"]}')

  status=$(echo "$response" | tail -n1)
  run_test "Update Document" 200 "$status"
}

# Test 9: Get Statistics
test_get_statistics() {
  print_info "Test 9: Get Statistics"

  response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}${API_PREFIX}/statistics" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Get Statistics" 200 "$status"
}

# Test 10: Download Document
test_download_document() {
  print_info "Test 10: Download Document"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}/download" \
    -H "Authorization: Bearer ${TOKEN}" \
    -o /tmp/downloaded-doc.txt)

  status=$(echo "$response" | tail -n1)
  run_test "Download Document" 200 "$status"

  # Cleanup
  rm -f /tmp/downloaded-doc.txt
}

# Test 11: Delete Document
test_delete_document() {
  print_info "Test 11: Delete Document"

  if [ -z "$DOCUMENT_ID" ]; then
    print_error "No document ID available"
    return
  fi

  response=$(curl -s -w "\n%{http_code}" -X DELETE \
    "${BASE_URL}${API_PREFIX}/${DOCUMENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}")

  status=$(echo "$response" | tail -n1)
  run_test "Delete Document" 204 "$status"
}

# Print test summary
print_summary() {
  echo ""
  echo "======================================"
  echo "       Test Summary"
  echo "======================================"
  echo "Total Tests:  $TOTAL_TESTS"
  print_success "Passed:       $PASSED_TESTS"
  print_error "Failed:       $FAILED_TESTS"
  echo "======================================"

  if [ "$FAILED_TESTS" -eq 0 ]; then
    print_success "All tests passed!"
    exit 0
  else
    print_error "Some tests failed!"
    exit 1
  fi
}

# Main execution
main() {
  echo "======================================"
  echo "  Document Archiving API Test"
  echo "======================================"
  echo "Base URL: $BASE_URL"
  echo ""

  authenticate

  test_upload_document
  test_get_document
  test_search_documents
  test_get_all_documents
  test_archive_document
  test_restore_document
  test_get_document_content
  test_update_document
  test_get_statistics
  test_download_document
  test_delete_document

  print_summary
}

# Run main function
main
