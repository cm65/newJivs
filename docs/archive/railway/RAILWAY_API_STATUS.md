# Railway Deployment - API Status Report

## 🌐 Live Application URLs

**Frontend:** https://jivs-frontend-production.up.railway.app
**Backend:** https://jivs-backend-production.up.railway.app
**Swagger UI:** https://jivs-backend-production.up.railway.app/swagger-ui/index.html

**Login:** admin / password ✅ **WORKING**

---

## ✅ Working APIs (Tested)

### Authentication
- ✅ `POST /api/v1/auth/login` - Working perfectly
- ✅ `POST /api/v1/auth/refresh` - Should work (JWT refresh)
- ⚠️ `POST /api/v1/auth/register` - Endpoint exists but may have validation issues

### Health & Monitoring
- ✅ `GET /actuator/health` - Working (200 OK)
- ✅ Database connection - PostgreSQL connected and healthy

---

## ⚠️ APIs with Potential Issues

### Document Upload (MAIN ISSUE)
- ⚠️ `POST /api/v1/documents/upload` - **LIKELY FAILING**

**Problem:** Storage configuration
- Uses local filesystem: `/var/jivs/storage`
- Railway containers are ephemeral
- Files may upload but disappear on restart
- **Need Railway Volume or cloud storage**

**Fix:** Add Railway Volume at `/var/jivs/storage`

---

### Other Document APIs (Untested)
- ❓ `GET /api/v1/documents` - List documents
- ❓ `GET /api/v1/documents/{id}` - Get document details
- ❓ `GET /api/v1/documents/{id}/download` - Download document
- ❓ `PUT /api/v1/documents/{id}` - Update document
- ❓ `DELETE /api/v1/documents/{id}` - Delete document
- ❓ `POST /api/v1/documents/{id}/archive` - Archive document

**Status:** Endpoints exist, but depend on working storage

---

## 🔧 Configuration Issues

### 1. Storage Backend
**Current:**
```yaml
storage:
  provider: local
  base-path: /var/jivs/storage  # ❌ Ephemeral in Railway!
```

**Recommended:**
- Option 1: Add Railway Volume (quick fix)
- Option 2: Use Cloudflare R2 (production)
- Option 3: Use AWS S3 (enterprise)

---

### 2. File Size Limits
**Potential Issue:** Default Spring Boot limit is 1MB

**Check in logs for:**
- "Maximum upload size exceeded"
- "FileSizeLimitExceededException"

**Fix if needed:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

---

### 3. Request Timeout
**Railway Default:** 60 seconds for HTTP requests

**May affect:**
- Large file uploads
- Long-running migrations
- Batch data quality checks

**Workaround:** Use chunked uploads or background jobs

---

## 📋 Complete API Inventory

### Authentication & User Management
- POST /api/v1/auth/login ✅
- POST /api/v1/auth/register ⚠️
- POST /api/v1/auth/refresh ❓
- POST /api/v1/auth/logout ❓
- GET /api/v1/users ❓
- PUT /api/v1/users/{id} ❓

### Documents
- POST /api/v1/documents/upload ⚠️ **MAIN ISSUE**
- GET /api/v1/documents ❓
- GET /api/v1/documents/{id} ❓
- PUT /api/v1/documents/{id} ❓
- DELETE /api/v1/documents/{id} ❓
- POST /api/v1/documents/{id}/archive ❓
- GET /api/v1/documents/{id}/download ❓

### Data Extraction
- POST /api/v1/extractions ❓
- GET /api/v1/extractions ❓
- GET /api/v1/extractions/{id} ❓
- POST /api/v1/extractions/{id}/start ❓
- POST /api/v1/extractions/{id}/pause ❓
- POST /api/v1/extractions/{id}/cancel ❓

### Data Migration
- POST /api/v1/migrations ❓
- GET /api/v1/migrations ❓
- GET /api/v1/migrations/{id} ❓
- POST /api/v1/migrations/{id}/start ❓
- POST /api/v1/migrations/{id}/rollback ❓

### Data Quality
- POST /api/v1/data-quality/rules ❓
- GET /api/v1/data-quality/rules ❓
- POST /api/v1/data-quality/profile ❓
- GET /api/v1/data-quality/issues ❓

### Compliance (GDPR/CCPA)
- POST /api/v1/compliance/requests ❓
- GET /api/v1/compliance/requests ❓
- GET /api/v1/compliance/requests/{id} ❓
- POST /api/v1/compliance/requests/{id}/process ❓

### Analytics & Reporting
- GET /api/v1/analytics/dashboard ❓
- GET /api/v1/analytics/reports ❓
- POST /api/v1/analytics/reports ❓

### Health & Monitoring
- GET /actuator/health ✅
- GET /actuator/metrics ❓
- GET /actuator/info ❓

---

## 🚨 Critical Path to Fix Upload

### Step 1: Add Railway Volume (5 minutes)
1. Railway Dashboard → Backend Service
2. Click "Volumes" tab
3. Click "New Volume"
4. Mount path: `/var/jivs/storage`
5. Size: 1GB (free tier)
6. Click "Add Volume"
7. Service will auto-redeploy

### Step 2: Test Upload (2 minutes)
1. Login to frontend
2. Try uploading a document
3. Check if file persists
4. Redeploy backend and check if file still exists

### Step 3: Verify Persistence
```bash
# Check Railway logs for storage-related errors
# Look for: "Successfully saved file to: /var/jivs/storage/..."
```

---

## 🔍 How to Debug Upload Issues

### Get Upload Error Details

**Method 1: Browser DevTools**
1. Open Frontend in browser
2. Open DevTools (F12) → Network tab
3. Try upload
4. Check failed request → Response tab
5. Look for error message

**Method 2: Direct API Test**
```bash
# Get token
TOKEN=$(curl -s -X POST https://jivs-backend-production.up.railway.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  jq -r '.data.accessToken')

# Test upload
curl -v -X POST https://jivs-backend-production.up.railway.app/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt" \
  -F "title=Test Document" \
  -F "description=Test upload"
```

**Method 3: Check Railway Logs**
1. Railway Dashboard → Backend Service
2. Click "Deployments" tab
3. Click latest deployment
4. Click "View Logs"
5. Search for "upload" or "storage" or "error"

---

## 📊 API Response Formats

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": [2025, 10, 21, 10, 30, 0, 0]
}
```

### Error Response
```json
{
  "timestamp": [2025, 10, 21, 10, 30, 0, 0],
  "status": 500,
  "error": "Internal Server Error",
  "message": "Detailed error message",
  "path": "/api/v1/documents/upload"
}
```

---

## 🎯 Priority Actions

**Priority 1 (Immediate):**
- ✅ Login working
- ⚠️ **Fix upload** - Add Railway Volume

**Priority 2 (Today):**
- Test all document APIs
- Verify file persistence
- Check file size limits

**Priority 3 (This Week):**
- Evaluate cloud storage (R2/S3)
- Test extraction and migration APIs
- Load test with concurrent uploads

---

## 📝 Testing Checklist

### Document Upload Flow
- [ ] Login with admin credentials
- [ ] Navigate to Documents page
- [ ] Click "Upload Document"
- [ ] Select file (<1MB for initial test)
- [ ] Fill in title and description
- [ ] Click "Upload"
- [ ] Verify success message
- [ ] Refresh page - file should appear in list
- [ ] Redeploy backend - file should still exist
- [ ] Try downloading file

### Common Upload Issues
- [ ] 401 Unauthorized → Token expired, login again
- [ ] 413 Payload Too Large → File size limit exceeded
- [ ] 500 Internal Server Error → Check storage permissions
- [ ] Network timeout → File too large or slow connection
- [ ] File disappears after redeploy → Need Railway Volume

---

## 🛠️ Quick Fixes

### Issue: "File too large"
```yaml
# Add to application-production.yml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### Issue: "Permission denied"
```bash
# Check Dockerfile creates directory with correct ownership
RUN mkdir -p /var/jivs/storage && chown -R jivs:jivs /var/jivs
```

### Issue: "Files disappear after restart"
```
→ Add Railway Volume at /var/jivs/storage
```

---

**Last Updated:** October 21, 2025
**Next Review:** After adding Railway Volume and testing upload
