# 🚨 CRITICAL WORKFLOW RULES - NEVER SKIP

## Rule #1: ALWAYS Run Continuous Tester Before ANY Code Changes

**MANDATORY**: Before writing, modifying, or refactoring ANY code, the continuous tester MUST be running in the background.

### When to Start Continuous Tester:
- ✅ Before writing new features
- ✅ Before fixing bugs
- ✅ Before refactoring code
- ✅ Before modifying tests
- ✅ Before changing configuration
- ✅ Before updating dependencies
- ✅ At the start of EVERY coding session

### How to Start:
```bash
# Option 1: Full continuous monitoring (RECOMMENDED)
cd /Users/chandramahadevan/jivs-platform
bash scripts/continuous-tester.sh --watch > /tmp/continuous-watch.log 2>&1 &

# Option 2: Quick status check first
bash scripts/continuous-tester.sh --status

# Option 3: Pre-commit validation
bash scripts/continuous-tester.sh --pre-commit
```

### Verify It's Running:
```bash
# Check if monitoring is active
ps aux | grep continuous-tester

# View real-time logs
tail -f /tmp/continuous-test-report.log
tail -f /tmp/test-alerts.log
```

### Why This Matters:
- **Prevents breaking changes** from entering the codebase
- **Catches issues immediately** instead of hours later
- **Ensures seamless development** with instant feedback
- **Maintains system health** at all times
- **Saves debugging time** by catching errors early

### What Gets Monitored:
1. Backend health (every 30 seconds)
2. Frontend accessibility (every 30 seconds)
3. Build status (on file changes)
4. API endpoints (every 2 minutes)
5. Critical UI paths (every 5 minutes)
6. File changes (real-time)

---

## Rule #2: Check Test Reports Before Committing

**MANDATORY**: Always check test reports before making git commits.

```bash
# Check for recent failures
tail -50 /tmp/continuous-test-report.log | grep ERROR

# Check alerts
cat /tmp/test-alerts.log

# Run pre-commit validation
bash scripts/continuous-tester.sh --pre-commit
```

---

## Rule #3: Never Disable Background Testing

The continuous tester should ONLY be stopped when:
- Shutting down the development environment completely
- Running full test suite with `--full` mode
- Debugging test infrastructure itself

**NEVER** stop it just because:
- ❌ Tests are failing (fix the code instead)
- ❌ It's "in the way" (it runs in background)
- ❌ You're making "quick changes" (those break things most often)

---

## Enforcement

This rule is enforced by:
1. This document (`.claude/WORKFLOW_RULES.md`)
2. CLAUDE.md documentation
3. Git pre-commit hooks (optional)
4. Continuous reminders in development workflow

---

**Last Updated**: 2025-10-13
**Status**: ACTIVE AND MANDATORY
**Compliance**: 100% Required
