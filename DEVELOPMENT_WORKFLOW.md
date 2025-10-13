# 🚨 JiVS Development Workflow - READ FIRST

## CRITICAL RULE #1: Continuous Testing ALWAYS Running

**Before you write, modify, or refactor ANY code:**

```bash
# Start continuous monitoring
cd /Users/chandramahadevan/jivs-platform
bash scripts/continuous-tester.sh --watch > /tmp/continuous-watch.log 2>&1 &
```

**Why?**
- Catches breaking changes immediately
- Prevents bugs from entering codebase
- Provides instant feedback on code changes
- Saves hours of debugging time

**Verify it's running:**
```bash
ps aux | grep continuous-tester
tail -f /tmp/continuous-test-report.log
```

---

## Development Workflow

### 1. Start Your Session
```bash
# Check system status
bash scripts/continuous-tester.sh --status

# Start continuous monitoring (if not already running)
bash scripts/continuous-tester.sh --watch > /tmp/continuous-watch.log 2>&1 &
```

### 2. Make Your Changes
- Write code
- Continuous tester monitors in background
- Check logs for any issues: `tail -f /tmp/test-alerts.log`

### 3. Before Committing
```bash
# Run pre-commit validation
bash scripts/continuous-tester.sh --pre-commit

# If all tests pass, commit
git add .
git commit -m "your message"
```

### 4. After Major Changes
```bash
# Run full test suite
bash scripts/continuous-tester.sh --full
```

---

## Quick Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| `--status` | Quick health check | <5s |
| `--quick` | Fast smoke tests | <2 min |
| `--watch` | Continuous monitoring | Ongoing |
| `--pre-commit` | Pre-commit validation | ~1 min |
| `--full` | Complete test suite | 15-20 min |

---

## Monitoring Logs

```bash
# All test results
tail -f /tmp/continuous-test-report.log

# Critical alerts only
tail -f /tmp/test-alerts.log

# Monitoring status
tail -f /tmp/continuous-watch.log
```

---

## What Gets Monitored

The continuous tester monitors:
- ✅ Backend health (every 30s)
- ✅ Frontend accessibility (every 30s)
- ✅ Build status (on file changes)
- ✅ API endpoints (every 2 min)
- ✅ Critical UI paths (every 5 min)
- ✅ File changes (real-time)

---

## NEVER Skip Continuous Testing

The tester should ONLY be stopped when:
- Shutting down the entire development environment
- Running full test suite with `--full` mode
- Debugging test infrastructure itself

**NEVER** stop it because:
- ❌ Tests are failing (fix the code instead)
- ❌ It's "in the way" (runs silently in background)
- ❌ Making "quick changes" (those break things most)

---

## Documentation

- **Workflow Rules**: [.claude/WORKFLOW_RULES.md](.claude/WORKFLOW_RULES.md)
- **Main Guide**: [CLAUDE.md](CLAUDE.md)
- **All Docs**: [docs/INDEX.md](docs/INDEX.md)

---

**Last Updated**: 2025-10-13
**Status**: MANDATORY AND ENFORCED
