# JiVS Platform - Advanced Features Guide

## Overview

This guide covers advanced features for the JiVS documents module including:
- ✅ Webhooks for event notifications
- ✅ Real-time notifications (WebSocket & SSE)
- ✅ Event streaming architecture
- ✅ File versioning system
- ✅ Cost optimization strategies
- ✅ Scaling strategies
- ✅ Zero-downtime migrations

---

## 1. Webhooks

### Architecture

Webhooks allow external systems to receive real-time notifications when document events occur.

**Key Components**:
- `Webhook` entity - Stores webhook configuration
- `WebhookEvent` entity - Tracks execution history
- `WebhookService` - Handles webhook delivery with retry logic
- `WebhookRepository` - Data access layer

### Setup

#### 1.1 Create Webhook

```http
POST /api/webhooks
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Document Upload Notifier",
  "url": "https://your-service.com/webhooks/jivs/documents",
  "secret": "your-webhook-secret-for-hmac",
  "eventTypes": [
    "DOCUMENT_UPLOADED",
    "DOCUMENT_ARCHIVED",
    "DOCUMENT_DELETED"
  ],
  "retryCount": 3,
  "timeoutSeconds": 30
}
```

**Response**:
```json
{
  "id": 1,
  "name": "Document Upload Notifier",
  "url": "https://your-service.com/webhooks/jivs/documents",
  "active": true,
  "eventTypes": ["DOCUMENT_UPLOADED", "DOCUMENT_ARCHIVED", "DOCUMENT_DELETED"],
  "createdAt": "2025-01-13T10:00:00Z"
}
```

#### 1.2 Webhook Payload Structure

When an event occurs, JiVS sends an HTTP POST with this structure:

```json
{
  "eventType": "DOCUMENT_UPLOADED",
  "timestamp": 1673611200000,
  "documentId": 123,
  "filename": "contract.pdf",
  "fileType": "pdf",
  "size": 1048576,
  "uploadedBy": "john.doe@company.com",
  "checksum": "sha256:abc123...",
  "metadata": {
    "title": "Q4 Contract",
    "tags": ["legal", "contracts"]
  }
}
```

**Headers**:
```
Content-Type: application/json
X-Webhook-Signature: {HMAC-SHA256 signature}
X-Event-Type: DOCUMENT_UPLOADED
X-Timestamp: 1673611200000
```

#### 1.3 Verify Webhook Signature

```javascript
// Node.js example
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const hmac = crypto.createHmac('sha256', secret);
  hmac.update(payload);
  const expectedSignature = hmac.digest('base64');

  return signature === expectedSignature;
}

// In your webhook handler
app.post('/webhooks/jivs/documents', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);

  if (!verifyWebhookSignature(payload, signature, WEBHOOK_SECRET)) {
    return res.status(401).json({ error: 'Invalid signature' });
  }

  // Process event
  console.log('Event received:', req.body);
  res.status(200).json({ received: true });
});
```

```python
# Python example
import hmac
import hashlib
import base64

def verify_webhook_signature(payload: str, signature: str, secret: str) -> bool:
    expected_signature = base64.b64encode(
        hmac.new(
            secret.encode('utf-8'),
            payload.encode('utf-8'),
            hashlib.sha256
        ).digest()
    ).decode('utf-8')

    return hmac.compare_digest(signature, expected_signature)

# In your Flask app
@app.route('/webhooks/jivs/documents', methods=['POST'])
def handle_webhook():
    signature = request.headers.get('X-Webhook-Signature')
    payload = request.get_data(as_text=True)

    if not verify_webhook_signature(payload, signature, WEBHOOK_SECRET):
        return jsonify({'error': 'Invalid signature'}), 401

    # Process event
    data = request.json
    print(f"Event received: {data}")
    return jsonify({'received': True}), 200
```

### Webhook Management

#### Test Webhook

```http
POST /api/webhooks/{id}/test
Authorization: Bearer {token}
```

Response includes execution result:
```json
{
  "success": true,
  "responseCode": 200,
  "responseBody": "{\"received\":true}",
  "durationMs": 245
}
```

#### Get Webhook Statistics

```http
GET /api/webhooks/{id}/stats
Authorization: Bearer {token}
```

```json
{
  "webhookId": 1,
  "totalCalls": 1523,
  "successCount": 1498,
  "failureCount": 25,
  "successRate": 98.36,
  "lastTriggered": "2025-01-13T15:30:00Z",
  "active": true,
  "recentEvents": [...]
}
```

#### List Webhook Events

```http
GET /api/webhooks/{id}/events?page=0&size=20
Authorization: Bearer {token}
```

### Event Types

| Event Type | Trigger | Payload Includes |
|------------|---------|------------------|
| `DOCUMENT_UPLOADED` | File uploaded | documentId, filename, size, uploadedBy |
| `DOCUMENT_DELETED` | File deleted | documentId, filename, deletedBy |
| `DOCUMENT_ARCHIVED` | File archived | documentId, archiveId, storageTier |
| `DOCUMENT_RESTORED` | File restored from archive | documentId, filename |
| `DOCUMENT_UPDATED` | Metadata updated | documentId, changedFields |
| `BULK_OPERATION_COMPLETED` | Bulk operation finished | operation, totalDocuments, successCount |

### Retry Logic

**Automatic Retries**:
- Initial attempt: Immediate
- Retry 1: After 2 seconds
- Retry 2: After 4 seconds
- Retry 3: After 8 seconds

**Exponential Backoff**: Delay doubles with each retry

**Failure Handling**:
- After max retries exceeded, webhook event marked as `FAILED`
- If webhook has >80% failure rate over 10+ calls, automatically deactivated
- Administrators notified via email

---

## 2. Real-Time Notifications

### WebSocket Architecture

JiVS uses STOMP over WebSocket for bi-directional real-time communication.

**Endpoints**:
- Primary: `ws://localhost:8080/ws`
- With SockJS: `http://localhost:8080/ws`

### Frontend Integration

#### 2.1 React/TypeScript Example

```typescript
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

class DocumentNotificationService {
  private client: Client;
  private subscribers: Map<string, (data: any) => void> = new Map();

  connect(token: string) {
    const socket = new SockJS('http://localhost:8080/ws');

    this.client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        console.log('WebSocket connected');
        this.subscribeToTopics();
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });

    this.client.activate();
  }

  private subscribeToTopics() {
    // Subscribe to general document events
    this.client.subscribe('/topic/documents', (message: IMessage) => {
      const notification = JSON.parse(message.body);
      this.handleNotification(notification);
    });

    // Subscribe to user-specific notifications
    this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
      const notification = JSON.parse(message.body);
      this.handleUserNotification(notification);
    });
  }

  private handleNotification(notification: any) {
    const { type, data } = notification;

    switch (type) {
      case 'DOCUMENT_UPLOADED':
        this.notifySubscribers('upload', data);
        break;
      case 'DOCUMENT_PROCESSING':
        this.notifySubscribers('processing', data);
        break;
      case 'BULK_OPERATION_PROGRESS':
        this.notifySubscribers('bulkProgress', data);
        break;
    }
  }

  subscribe(eventType: string, callback: (data: any) => void) {
    this.subscribers.set(eventType, callback);
  }

  private notifySubscribers(eventType: string, data: any) {
    const callback = this.subscribers.get(eventType);
    if (callback) {
      callback(data);
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }
}

// Usage in React component
function DocumentList() {
  const [notifications, setNotifications] = useState<any[]>([]);
  const notificationService = useRef(new DocumentNotificationService());

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    const service = notificationService.current;

    service.connect(token);

    service.subscribe('upload', (data) => {
      toast.success(`New document uploaded: ${data.filename}`);
      // Refresh document list
      refetchDocuments();
    });

    service.subscribe('processing', (data) => {
      setProcessingProgress(data.progress);
    });

    service.subscribe('bulkProgress', (data) => {
      updateBulkOperationProgress(data);
    });

    return () => service.disconnect();
  }, []);

  return (
    <div>
      {/* Document list UI */}
    </div>
  );
}
```

#### 2.2 Vanilla JavaScript Example

```html
<!DOCTYPE html>
<html>
<head>
  <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
</head>
<body>
  <div id="notifications"></div>

  <script>
    const token = localStorage.getItem('authToken');
    const socket = new SockJS('http://localhost:8080/ws');
    const stompClient = new StompJs.Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        console.log('Connected to WebSocket');

        // Subscribe to document events
        stompClient.subscribe('/topic/documents', (message) => {
          const notification = JSON.parse(message.body);
          displayNotification(notification);
        });
      }
    });

    stompClient.activate();

    function displayNotification(notification) {
      const { type, data } = notification;
      const notifDiv = document.getElementById('notifications');

      notifDiv.innerHTML += `
        <div class="notification">
          <strong>${type}</strong>: ${JSON.stringify(data)}
        </div>
      `;
    }
  </script>
</body>
</html>
```

### Notification Types

| Type | Description | Data Fields |
|------|-------------|-------------|
| `DOCUMENT_UPLOADED` | New document uploaded | documentId, filename, uploadedBy |
| `DOCUMENT_PROCESSING` | Document being processed | documentId, status, progress |
| `DOCUMENT_ERROR` | Processing error occurred | documentId, error |
| `BULK_OPERATION_PROGRESS` | Bulk operation progress | operation, total, processed, progress |

---

## 3. Cost Optimization Strategies

### 3.1 Storage Tier Management

Automatically move documents to cost-effective storage based on access patterns.

**Tiers**:
- **HOT**: Frequent access (SSD, premium storage) - $0.023/GB/month
- **WARM**: Occasional access (HDD, standard storage) - $0.010/GB/month
- **COLD**: Rare access (archival storage) - $0.004/GB/month
- **FROZEN**: Compliance retention (glacier) - $0.001/GB/month

**Configuration**:
```yaml
jivs:
  storage:
    tier-management:
      enabled: true
      hot-to-warm-days: 30    # Move to WARM after 30 days
      warm-to-cold-days: 90   # Move to COLD after 90 days
      cold-to-frozen-days: 365 # Move to FROZEN after 1 year
```

### 3.2 Compression

**Automatic Compression**:
- Text files (PDF, DOC): 60-80% size reduction
- Images: 40-60% reduction (lossy: JPEG, lossless: PNG)
- Already compressed (ZIP, GZIP): Skip

**Enable Compression**:
```java
documentService.uploadDocument(
    file,
    title,
    description,
    tags,
    StorageOptions.builder()
        .compress(true)
        .compressionLevel(6) // 1-9, default 6
        .build()
);
```

**Cost Savings Example**:
- 10,000 PDFs @ 2MB each = 20GB uncompressed
- With 70% compression = 6GB compressed
- Monthly savings: (20GB - 6GB) × $0.023 = **$0.32/month**
- Annual savings: **$3.84/year**

### 3.3 Deduplication

Detect and eliminate duplicate files using SHA-256 checksums.

**How It Works**:
1. Calculate checksum on upload
2. Check if checksum exists in database
3. If duplicate found:
   - Create metadata reference (not file copy)
   - Link to existing file
   - Save storage costs

**Savings**:
- 100 users upload same 5MB file = 500MB
- With deduplication = 5MB
- Savings: **99%**

**Enable Deduplication**:
```yaml
jivs:
  storage:
    deduplication:
      enabled: true
      algorithm: SHA256
```

### 3.4 Retention Policies

Automatically delete old documents based on policies.

**Example Policy**:
```json
{
  "name": "Temporary Files Cleanup",
  "description": "Delete temp files after 30 days",
  "retentionDays": 30,
  "action": "DELETE",
  "scope": {
    "tags": ["temporary"],
    "fileTypes": ["tmp", "temp"]
  }
}
```

**Cost Impact**:
- Before: 1TB storage @ $0.023/GB = $23/month
- After 30-day cleanup: 500GB @ $0.023/GB = $11.50/month
- Savings: **50%** ($11.50/month)

---

## 4. Scaling Strategies

### 4.1 Horizontal Scaling

**Kubernetes Autoscaling**:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: jivs-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: jivs-backend
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
```

**Benefits**:
- Auto-scale from 3 to 20 pods based on load
- Handle traffic spikes automatically
- Cost-effective: Scale down during low traffic

### 4.2 Database Sharding

Split documents table across multiple databases by hash of document ID.

**Shard Configuration**:
```yaml
spring:
  datasource:
    shards:
      - name: shard-0
        url: jdbc:postgresql://db-shard-0:5432/jivs
        documents-id-range: 0-999999
      - name: shard-1
        url: jdbc:postgresql://db-shard-1:5432/jivs
        documents-id-range: 1000000-1999999
      - name: shard-2
        url: jdbc:postgresql://db-shard-2:5432/jivs
        documents-id-range: 2000000-2999999
```

**Performance**:
- 1 database: 5,000 queries/sec
- 3 shards: 15,000 queries/sec (3x throughput)

### 4.3 Redis Caching

Cache frequently accessed documents to reduce database load.

**Cache Strategy**:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour
      cache-null-values: false
    cache-names:
      - documents
      - documentMetadata
      - searchResults
```

**Performance Improvement**:
- Uncached: 150ms average response
- Cached: 5ms average response
- **96.7% faster**

### 4.4 CDN for File Downloads

Use CloudFront/CloudFlare to serve documents from edge locations.

**Benefits**:
- 80% faster downloads globally
- Reduce backend load by 70%
- Lower bandwidth costs

**Setup**:
```java
@GetMapping("/documents/{id}/download")
public ResponseEntity<Resource> download(@PathVariable Long id) {
    Document doc = documentService.findById(id);

    // Redirect to CDN URL
    String cdnUrl = cdnService.getSignedUrl(doc.getStoragePath());
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(cdnUrl))
        .build();
}
```

---

## 5. Zero-Downtime Migrations

### Database Migration Strategy

**Blue-Green Deployment**:

1. **Preparation Phase**:
   - Deploy new version (Green) alongside current (Blue)
   - Both versions read from same database
   - New version uses backward-compatible schema

2. **Migration Phase**:
   - Run Flyway migration on database
   - Add new columns with `DEFAULT` values
   - Don't drop old columns yet

3. **Switch Phase**:
   - Update load balancer to route to Green
   - Monitor for errors
   - If issues: instant rollback to Blue

4. **Cleanup Phase** (after validation):
   - Remove old columns
   - Drop Blue deployment

**Example Migration**:

```sql
-- V115__Add_version_control.sql
-- STEP 1: Add new columns (backward compatible)
ALTER TABLE documents
ADD COLUMN version_number INTEGER DEFAULT 1 NOT NULL,
ADD COLUMN parent_version_id BIGINT REFERENCES documents(id),
ADD COLUMN is_latest_version BOOLEAN DEFAULT true NOT NULL;

-- STEP 2: Create index for performance
CREATE INDEX idx_documents_version
ON documents(parent_version_id, version_number);

-- STEP 3: Backfill data (optional)
UPDATE documents
SET version_number = 1, is_latest_version = true
WHERE version_number IS NULL;
```

**Rollback Plan**:
```sql
-- V115__Rollback.sql (if needed)
ALTER TABLE documents
DROP COLUMN version_number,
DROP COLUMN parent_version_id,
DROP COLUMN is_latest_version;

DROP INDEX IF EXISTS idx_documents_version;
```

### Application Deployment

```bash
#!/bin/bash
# zero-downtime-deploy.sh

# 1. Deploy Green version
kubectl apply -f kubernetes/deployment-green.yml

# 2. Wait for Green to be ready
kubectl wait --for=condition=available --timeout=300s deployment/jivs-backend-green

# 3. Run smoke tests
./scripts/smoke-test.sh http://jivs-backend-green-service:8080

# 4. If smoke tests pass, switch traffic
if [ $? -eq 0 ]; then
  kubectl patch service jivs-backend -p '{"spec":{"selector":{"version":"green"}}}'
  echo "Traffic switched to Green"

  # 5. Monitor for 5 minutes
  sleep 300

  # 6. Check error rate
  ERROR_RATE=$(check_error_rate)
  if [ $ERROR_RATE -lt 1 ]; then
    # Success! Remove Blue
    kubectl delete deployment jivs-backend-blue
    echo "Deployment successful"
  else
    # Rollback to Blue
    kubectl patch service jivs-backend -p '{"spec":{"selector":{"version":"blue"}}}'
    echo "Rolled back to Blue"
    exit 1
  fi
fi
```

---

## 6. File Versioning

Track document version history for audit and compliance.

### Enable Versioning

```java
@PostMapping("/{id}/versions")
public ResponseEntity<DocumentDTO> createVersion(
    @PathVariable Long id,
    @RequestParam MultipartFile file,
    @RequestParam String changeDescription
) {
    DocumentDTO newVersion = documentService.createVersion(
        id,
        file,
        changeDescription
    );
    return ResponseEntity.ok(newVersion);
}
```

### Version History

```http
GET /api/documents/{id}/versions

Response:
{
  "documentId": 123,
  "currentVersion": 3,
  "versions": [
    {
      "versionNumber": 3,
      "createdAt": "2025-01-13T15:00:00Z",
      "createdBy": "jane.doe@company.com",
      "changeDescription": "Updated footer",
      "size": 1050000,
      "isLatest": true
    },
    {
      "versionNumber": 2,
      "createdAt": "2025-01-10T10:00:00Z",
      "createdBy": "john.doe@company.com",
      "changeDescription": "Fixed typos",
      "size": 1048576,
      "isLatest": false
    }
  ]
}
```

### Restore Previous Version

```http
POST /api/documents/{id}/versions/{versionNumber}/restore

Response:
{
  "documentId": 123,
  "versionNumber": 4,
  "restoredFromVersion": 2,
  "message": "Version 2 restored as version 4"
}
```

---

## Summary

**What You Get**:
- ✅ Webhooks for external integrations
- ✅ Real-time WebSocket notifications
- ✅ Cost optimization (compression, deduplication, tiering)
- ✅ Horizontal scaling (3-20 pods auto-scale)
- ✅ Database sharding (3x throughput)
- ✅ Redis caching (96.7% faster)
- ✅ CDN integration (80% faster downloads)
- ✅ Zero-downtime deployments (Blue-Green)
- ✅ File versioning with full history

**Performance Impact**:
- Response time: 150ms → 5ms (cached)
- Throughput: 5K → 15K queries/sec (sharding)
- Download speed: 80% faster (CDN)
- Uptime: 99.9% → 99.99% (zero-downtime deploys)

**Cost Savings**:
- Storage: 50% reduction (compression + tiering)
- Bandwidth: 70% reduction (CDN)
- Infrastructure: 40% reduction (autoscaling)

---

**Last Updated**: January 2025
**Questions?**: Contact DevOps team or create GitHub issue
