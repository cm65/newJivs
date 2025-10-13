# Quick Start: Contract Testing with Pact

**Goal**: Catch frontend-backend API mismatches in seconds, not hours
**Time to Implement**: 2-4 hours for first contract
**Impact**: Would have caught the migration bug immediately

---

## 🎯 What Is Contract Testing?

**Simple Explanation**:
- Frontend says: "I will send this data format"
- Backend says: "I expect this data format"
- Contract test ensures: They match!

**Migration Bug Example**:
```
❌ Without Contract Testing:
Frontend sends:  { name: "...", sourceConfig: {} }
Backend expects: { name: "...", sourceSystem: "..." }
Result: Bug discovered by user clicking buttons (2 hours wasted)

✅ With Contract Testing:
Contract test runs in 5 seconds:
  ❌ FAIL: Frontend sends 'sourceConfig', backend expects 'sourceSystem'
Result: Developer fixes immediately (1 minute)
```

---

## 📦 Installation (30 minutes)

### Backend Setup (Spring Boot):

**Step 1**: Add Pact dependency
```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>au.com.dius.pact.provider</groupId>
    <artifactId>junit5spring</artifactId>
    <version>4.6.3</version>
    <scope>test</scope>
</dependency>
```

**Step 2**: Run Maven update
```bash
cd backend
mvn clean install
```

---

### Frontend Setup (React + TypeScript):

**Step 1**: Install Pact
```bash
cd frontend
npm install --save-dev @pact-foundation/pact
```

**Step 2**: Add test script
```json
// package.json
{
  "scripts": {
    "test:contracts": "jest --testMatch='**/*.pact.test.ts'"
  }
}
```

---

## 🧪 Your First Contract Test (1 hour)

### Step 1: Frontend Consumer Test

Create `frontend/src/services/__tests__/migrationService.pact.test.ts`:

```typescript
import { Pact, Matchers } from '@pact-foundation/pact';
import path from 'path';
import migrationService from '../migrationService';

describe('Migration API Contract', () => {
  // 1. Set up Pact provider
  const provider = new Pact({
    consumer: 'JiVS Frontend',
    provider: 'JiVS Backend',
    port: 8080,
    log: path.resolve(process.cwd(), 'logs', 'pact.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'info',
  });

  // 2. Start mock server before tests
  beforeAll(() => provider.setup());

  // 3. Stop mock server after tests
  afterAll(() => provider.finalize());

  // 4. Clear interactions between tests
  afterEach(() => provider.verify());

  // 5. THE ACTUAL CONTRACT TEST
  describe('POST /api/v1/migrations', () => {
    it('should create migration with correct request format', async () => {
      // Define expected interaction
      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create migration',
        withRequest: {
          method: 'POST',
          path: '/api/v1/migrations',
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            name: 'Test Migration',
            description: 'Test description',
            sourceSystem: 'Oracle',         // ← MUST match backend
            targetSystem: 'PostgreSQL',     // ← MUST match backend
            migrationType: 'FULL',          // ← MUST match backend
          },
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json',
          },
          body: Matchers.like({
            id: Matchers.uuid(),
            name: 'Test Migration',
            status: 'INITIALIZED',
            sourceSystem: 'Oracle',
            targetSystem: 'PostgreSQL',
            migrationType: 'FULL',
            createdAt: Matchers.iso8601DateTime(),
          }),
        },
      });

      // Call your actual service
      const response = await migrationService.createMigration({
        name: 'Test Migration',
        description: 'Test description',
        sourceSystem: 'Oracle',
        targetSystem: 'PostgreSQL',
        migrationType: 'FULL',
      });

      // Verify response
      expect(response.id).toBeDefined();
      expect(response.name).toBe('Test Migration');
      expect(response.status).toBe('INITIALIZED');
    });
  });
});
```

**Run the test**:
```bash
npm run test:contracts
```

**Output**:
```
✅ PASS  src/services/__tests__/migrationService.pact.test.ts
  Migration API Contract
    POST /api/v1/migrations
      ✓ should create migration with correct request format (156 ms)

Pact file written to: pacts/jivs_frontend-jivs_backend.json
```

---

### Step 2: Backend Provider Test

Create `backend/src/test/java/com/jivs/platform/contract/MigrationContractTest.java`:

```java
package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")  // Points to frontend pact files
@ExtendWith(SpringExtension.class)
public class MigrationContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("user is authenticated")
    public void authenticatedUser() {
        // Set up authenticated state
        // (In real test, create a user and generate JWT token)
        System.out.println("Setting up authenticated user state");
    }
}
```

**Run the test**:
```bash
mvn test -Dtest=MigrationContractTest
```

**Output**:
```
[INFO] Running com.jivs.platform.contract.MigrationContractTest
[INFO]
Verifying a pact between JiVS Frontend and JiVS Backend
  [Using File ../frontend/pacts/jivs_frontend-jivs_backend.json]
  Given user is authenticated
  a request to create migration
    returns a response which
      has status code 201 (OK)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (OK)

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.456 s
[INFO] BUILD SUCCESS
```

---

## 🚨 What Happens When Contract Breaks?

### Scenario: Developer changes frontend to send wrong field

**Frontend change**:
```typescript
// Developer accidentally uses old field name
await migrationService.createMigration({
  name: 'Test',
  sourceConfig: {},  // ← Wrong! Should be sourceSystem
});
```

**Contract test runs**:
```bash
npm run test:contracts
```

**Output** ❌:
```
FAIL  src/services/__tests__/migrationService.pact.test.ts
  Migration API Contract
    POST /api/v1/migrations
      ✗ should create migration with correct request format (89 ms)

Error: Request body does not match contract:
  Expected: { name, sourceSystem, targetSystem, migrationType }
  Actual:   { name, sourceConfig }

  Missing fields: sourceSystem, targetSystem, migrationType
  Unexpected fields: sourceConfig

  Fix: Update request to match contract or update contract if API changed
```

**Developer fixes immediately** (1 minute):
```typescript
// Fixed!
await migrationService.createMigration({
  name: 'Test',
  sourceSystem: 'Oracle',
  targetSystem: 'PostgreSQL',
  migrationType: 'FULL',
});
```

---

## 🔄 Continuous Testing Workflow

### Daily Developer Workflow:

```
09:00 AM - Start work
  ↓
09:05 AM - Modify MigrationController
  ↓
09:05:05 AM - Watch mode runs tests (auto)
  ↓
  ├─ Contract tests (5s) ✅
  ├─ Integration tests (8s) ✅
  └─ Unit tests (5s) ✅
  ↓
09:05:20 AM - All green! ✅
  ↓
09:10 AM - Commit changes
  ↓
09:10:30 AM - CI/CD runs full suite ✅
  ↓
09:11 AM - Deploy to staging ✅
  ↓
Result: Zero bugs, zero manual testing!
```

---

## 📊 Benefits Comparison

| Scenario | Without Contract Tests | With Contract Tests |
|----------|----------------------|---------------------|
| **Time to detect bug** | Hours-Days (manual testing) | Seconds (automatic) |
| **Developer effort** | High (debugging, clicking) | Low (test tells you exactly what's wrong) |
| **User impact** | User sees errors | User never sees errors |
| **Confidence** | Low (might miss bugs) | High (tests verify contract) |
| **Cost** | 2-3 hours to fix + user frustration | 1 minute to fix |

---

## 🎯 Next Steps

### For Your Project:

1. **Today** (2 hours):
   - [ ] Set up Pact in frontend + backend
   - [ ] Write first contract test (Migration API)
   - [ ] Run and verify it works

2. **This Week** (8 hours):
   - [ ] Add contracts for 5 critical APIs
   - [ ] Integrate into CI/CD
   - [ ] Set up watch mode

3. **This Month** (20 hours):
   - [ ] Add contracts for all 60 endpoints
   - [ ] Achieve 100% contract coverage
   - [ ] Train team on contract testing

---

## 🆘 Troubleshooting

### Issue: "Pact file not found"
```bash
# Make sure pact file is generated
ls frontend/pacts/  # Should see jivs_frontend-jivs_backend.json
```

### Issue: "Provider verification fails"
```bash
# Check backend is using correct request format
# Compare Pact file with actual controller code
```

### Issue: "State setup fails"
```java
@State("user is authenticated")
public void authenticatedUser() {
    // Ensure this method sets up the required state
    // e.g., create user, generate JWT, etc.
}
```

---

## 📚 Resources

- **Pact Docs**: https://docs.pact.io/
- **Pact Workshop**: https://docs.pact.io/implementation_guides/workshops
- **Spring Boot + Pact**: https://docs.pact.io/implementation_guides/jvm/provider/junit5spring
- **TypeScript + Pact**: https://github.com/pact-foundation/pact-js

---

**Created**: January 2025
**Estimated Setup Time**: 2-4 hours
**Expected Impact**: 90% reduction in API contract bugs
**ROI**: Break-even after first prevented bug (usually < 1 week)

---

**Next**: See `CONTINUOUS_TESTING_ARCHITECTURE.md` for full 6-layer strategy
