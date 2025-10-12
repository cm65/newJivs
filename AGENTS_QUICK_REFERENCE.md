# JiVS Platform - Agent Quick Reference Guide

## 🎯 Priority Matrix

```
HIGH PRIORITY (13 agents) → Adapt Immediately
├─ CRITICAL (7) → Phase 1 (Week 1-2)
│  ├─ backend-architect.md ⭐⭐⭐⭐⭐
│  ├─ devops-automator.md ⭐⭐⭐⭐⭐
│  ├─ api-tester.md ⭐⭐⭐⭐⭐
│  ├─ test-writer-fixer.md ⭐⭐⭐⭐⭐
│  ├─ legal-compliance-checker.md ⭐⭐⭐⭐⭐
│  ├─ frontend-developer.md ⭐⭐⭐⭐
│  └─ analytics-reporter.md ⭐⭐⭐⭐
│
└─ HIGH (6) → Phase 2-3 (Week 3-6)
   ├─ performance-benchmarker.md ⭐⭐⭐⭐
   ├─ test-results-analyzer.md ⭐⭐⭐⭐
   ├─ infrastructure-maintainer.md ⭐⭐⭐⭐
   ├─ workflow-optimizer.md ⭐⭐⭐
   ├─ sprint-prioritizer.md ⭐⭐⭐
   └─ project-shipper.md ⭐⭐⭐

MEDIUM PRIORITY (8 agents) → Adapt as Needed
LOW PRIORITY (6 agents) → Situational Use
NOT RECOMMENDED (10 agents) → Skip
```

---

## 📊 Complete Agent Inventory

| # | Agent Name | Priority | JiVS Use Case | Action |
|---|------------|----------|---------------|--------|
| **ENGINEERING** (7 agents) |
| 1 | backend-architect.md | ⭐⭐⭐⭐⭐ CRITICAL | Spring Boot API design, PostgreSQL schema, security | **ADAPT NOW** |
| 2 | devops-automator.md | ⭐⭐⭐⭐⭐ CRITICAL | K8s deployment, CI/CD, monitoring | **ADAPT NOW** |
| 3 | frontend-developer.md | ⭐⭐⭐⭐ HIGH | React components, Material-UI, Redux | **ADAPT NOW** |
| 4 | ai-engineer.md | ⭐⭐⭐ MEDIUM | Future AI features (anomaly detection, smart mapping) | Consider |
| 5 | mobile-app-builder.md | ❌ NOT REC | JiVS is web-only | Skip |
| 6 | rapid-prototyper.md | ⭐⭐ LOW-MED | Quick POCs for new connectors | Situational |
| **TESTING** (5 agents) |
| 7 | api-tester.md | ⭐⭐⭐⭐⭐ CRITICAL | Load testing, performance testing, API validation | **ADAPT NOW** |
| 8 | test-writer-fixer.md | ⭐⭐⭐⭐⭐ CRITICAL | E2E test maintenance (64 tests) | **ADAPT NOW** |
| 9 | performance-benchmarker.md | ⭐⭐⭐⭐ HIGH | Extraction/migration performance optimization | **ADAPT** |
| 10 | test-results-analyzer.md | ⭐⭐⭐⭐ HIGH | Quality metrics, test analysis | **ADAPT** |
| 11 | workflow-optimizer.md | ⭐⭐⭐ MED-HIGH | Development workflow efficiency | Consider |
| 12 | tool-evaluator.md | ⭐⭐⭐ MEDIUM | Evaluate new testing/monitoring tools | Consider |
| **DESIGN** (5 agents) |
| 13 | ui-designer.md | ⭐⭐ LOW | UI improvements (Material-UI) | Situational |
| 14 | ux-researcher.md | ⭐⭐ LOW | Enterprise UX research | Situational |
| 15 | brand-guardian.md | ⭐ LOW | Brand consistency | Skip |
| 16 | visual-storyteller.md | ⭐ LOW | Visual content | Skip |
| 17 | whimsy-injector.md | ❌ NOT REC | Not appropriate for enterprise | Skip |
| **MARKETING** (7 agents) |
| 18 | content-creator.md | ❌ NOT REC | Consumer app marketing | Skip |
| 19 | growth-hacker.md | ❌ NOT REC | Consumer growth tactics | Skip |
| 20 | instagram-curator.md | ❌ NOT REC | Social media | Skip |
| 21 | reddit-community-builder.md | ❌ NOT REC | Social media | Skip |
| 22 | tiktok-strategist.md | ❌ NOT REC | Social media | Skip |
| 23 | twitter-engager.md | ❌ NOT REC | Social media | Skip |
| 24 | app-store-optimizer.md | ❌ NOT REC | App store optimization | Skip |
| **PRODUCT** (3 agents) |
| 25 | feedback-synthesizer.md | ⭐⭐ LOW-MED | Client feedback analysis | Consider |
| 26 | sprint-prioritizer.md | ⭐⭐⭐ MEDIUM | Sprint planning, feature prioritization | **ADAPT** |
| 27 | trend-researcher.md | ⭐⭐ LOW-MED | Industry trends, compliance updates | Consider |
| **PROJECT MGMT** (3 agents) |
| 28 | experiment-tracker.md | ⭐⭐ LOW | A/B testing | Situational |
| 29 | project-shipper.md | ⭐⭐⭐ MEDIUM | Release management, launch coordination | **ADAPT** |
| 30 | studio-producer.md | ⭐⭐ LOW-MED | Project coordination | Consider |
| **OPERATIONS** (5 agents) |
| 31 | analytics-reporter.md | ⭐⭐⭐⭐ HIGH | Analytics dashboards, metrics reporting | **ADAPT NOW** |
| 32 | finance-tracker.md | ❌ NOT REC | Financial tracking | Skip |
| 33 | infrastructure-maintainer.md | ⭐⭐⭐⭐ HIGH | Monitoring, scaling, performance | **ADAPT** |
| 34 | legal-compliance-checker.md | ⭐⭐⭐⭐⭐ CRITICAL | GDPR/CCPA compliance validation | **ADAPT NOW** |
| 35 | support-responder.md | ⭐ LOW | Customer support automation | Skip |
| **BONUS** (2 agents) |
| 36 | joker.md | ❌ NOT REC | Humor (not enterprise-appropriate) | Skip |
| 37 | studio-coach.md | ⭐ LOW | Team coaching | Skip |

---

## 🚀 Implementation Timeline

### **Phase 1: Critical Infrastructure** (Week 1-2) - 4 agents
**Must-Have Foundation**

| Agent | JiVS Module | Priority | Effort |
|-------|-------------|----------|--------|
| backend-architect | Backend APIs (Extraction, Migration, DQ, Compliance) | P0 | 3 days |
| devops-automator | K8s, CI/CD, Monitoring | P0 | 3 days |
| api-tester | API Performance Testing | P0 | 2 days |
| legal-compliance-checker | Compliance Module | P0 | 2 days |

**Week 1-2 Deliverables:**
- ✅ Backend architect agent with JiVS Spring Boot patterns
- ✅ DevOps automation for K8s deployments
- ✅ Load testing suite for APIs
- ✅ Compliance validation workflows

---

### **Phase 2: Testing & Frontend** (Week 3-4) - 4 agents
**Quality & User Experience**

| Agent | JiVS Module | Priority | Effort |
|-------|-------------|----------|--------|
| test-writer-fixer | E2E Test Maintenance (64 tests) | P1 | 2 days |
| frontend-developer | React Frontend Components | P1 | 3 days |
| performance-benchmarker | Performance Optimization | P1 | 2 days |
| test-results-analyzer | Quality Metrics & Reporting | P1 | 2 days |

**Week 3-4 Deliverables:**
- ✅ Automated E2E test maintenance
- ✅ React component development guidelines
- ✅ Performance benchmarking suite
- ✅ Quality metrics dashboard

---

### **Phase 3: Analytics & Operations** (Week 5-6) - 3 agents
**Observability & Optimization**

| Agent | JiVS Module | Priority | Effort |
|-------|-------------|----------|--------|
| analytics-reporter | Analytics & Reporting | P1 | 3 days |
| infrastructure-maintainer | System Monitoring & Scaling | P1 | 2 days |
| workflow-optimizer | Development Workflow Efficiency | P2 | 2 days |

**Week 5-6 Deliverables:**
- ✅ Analytics automation
- ✅ Infrastructure monitoring
- ✅ Workflow optimizations

---

### **Phase 4: Product & Planning** (Future) - 2 agents
**Strategic & Planning**

| Agent | JiVS Use Case | Priority | Effort |
|-------|---------------|----------|--------|
| sprint-prioritizer | Sprint Planning & Feature Prioritization | P2 | 1 day |
| project-shipper | Release Management | P2 | 1 day |

---

## 🎨 Agent Use Cases by JiVS Module

### **Extraction Module**
- **backend-architect** → Design extraction connector APIs
- **api-tester** → Load test extraction operations
- **performance-benchmarker** → Optimize extraction throughput
- **analytics-reporter** → Track extraction success rates

**Extraction Services:**
- ExtractionService
- JdbcConnector, SapConnector, FileConnector, ApiConnector
- ConnectionPoolManager

---

### **Migration Module**
- **backend-architect** → Design migration orchestration
- **api-tester** → Test migration workflows
- **performance-benchmarker** → Optimize migration performance
- **test-writer-fixer** → E2E tests for migration lifecycle

**Migration Services:**
- MigrationOrchestrator
- MigrationPhaseExecutor (7 phases)
- RollbackService

---

### **Data Quality Module**
- **backend-architect** → Design quality rule engine
- **test-writer-fixer** → Tests for quality rules
- **performance-benchmarker** → Profile quality checks
- **analytics-reporter** → Quality score dashboards

**Data Quality Services:**
- DataQualityService
- QualityRuleEngine
- DataProfilingService
- IssueTracker

---

### **Compliance Module**
- **legal-compliance-checker** → GDPR/CCPA validation
- **backend-architect** → Compliance API design
- **test-writer-fixer** → Compliance workflow tests
- **analytics-reporter** → Compliance metrics

**Compliance Services:**
- ComplianceService
- AuditService
- DataDiscoveryService
- ConsentManager

---

### **Frontend (React + TypeScript)**
- **frontend-developer** → Component development
- **test-writer-fixer** → E2E tests (Playwright)
- **performance-benchmarker** → Frontend performance

**Frontend Pages:**
- Dashboard, Extractions, Migrations, DataQuality, Compliance

---

### **DevOps & Infrastructure**
- **devops-automator** → K8s deployment automation
- **infrastructure-maintainer** → Monitoring & scaling
- **api-tester** → Load testing

**Infrastructure:**
- Kubernetes (StatefulSets, Deployments, HPA)
- PostgreSQL, Redis, Elasticsearch, RabbitMQ
- Prometheus + Grafana monitoring

---

## 📋 Agent Customization Checklist

For each agent you adapt, ensure:

- [ ] Replace Node.js/npm references with Java/Maven
- [ ] Update technology stack (Spring Boot, PostgreSQL, Redis, etc.)
- [ ] Add JiVS-specific modules and services
- [ ] Include JiVS API endpoints
- [ ] Remove mobile app references
- [ ] Remove social media/marketing content
- [ ] Add JiVS-specific examples with code references
- [ ] Update tool permissions
- [ ] Test agent with real JiVS tasks
- [ ] Document customizations

---

## 🎯 Quick Decision Tree

**"Which agent should I use?"**

```
Need to design an API?
  └─> backend-architect

Need to deploy/scale infrastructure?
  └─> devops-automator

Need to test API performance?
  └─> api-tester

Need to write or fix tests?
  └─> test-writer-fixer

Need to validate GDPR/CCPA compliance?
  └─> legal-compliance-checker

Need to optimize performance?
  └─> performance-benchmarker

Need to build React components?
  └─> frontend-developer

Need to analyze test results?
  └─> test-results-analyzer

Need to generate analytics reports?
  └─> analytics-reporter

Need to monitor/scale systems?
  └─> infrastructure-maintainer

Need to plan a sprint?
  └─> sprint-prioritizer

Need to manage a release?
  └─> project-shipper
```

---

## 📈 Expected Benefits

### **Development Velocity**
- ⚡ 30-40% faster API development with standardized patterns
- ⚡ 50% reduction in deployment time with automation
- ⚡ 60% faster test writing and maintenance

### **Quality Improvements**
- ✅ >80% code coverage (from test-writer-fixer)
- ✅ <5% bug escape rate (from comprehensive testing)
- ✅ <200ms API response times (from performance-benchmarker)

### **Operational Efficiency**
- 📊 Real-time monitoring and alerting
- 📊 Automated scaling based on load
- 📊 Cost optimization through right-sizing

### **Compliance**
- 🔒 100% GDPR/CCPA audit pass rate
- 🔒 <1 hour compliance request processing
- 🔒 Complete audit trail

---

## 🔗 Related Documentation

- **Full Analysis:** [AGENTS_ANALYSIS.md](./AGENTS_ANALYSIS.md) - Comprehensive 37-agent analysis
- **JiVS Architecture:** [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- **Claude Guide:** [CLAUDE.md](./CLAUDE.md) - Implementation guide
- **Testing Summary:** [TESTING_SUMMARY.md](./TESTING_SUMMARY.md) - E2E test status

---

## 🏁 Next Steps

1. **Review this guide** and [AGENTS_ANALYSIS.md](./AGENTS_ANALYSIS.md)
2. **Create `.claude/agents/` directory** in JiVS repository
3. **Start with Phase 1** (4 critical agents)
4. **Customize each agent** using the checklist above
5. **Test agents** on real JiVS development tasks
6. **Iterate** based on team feedback
7. **Expand to Phase 2-4** agents progressively

---

**Last Updated:** 2025-10-12
**Total Agents Analyzed:** 37
**Recommended for JiVS:** 13 (High Priority)
**Source:** agents-main.zip from Contains Studio
