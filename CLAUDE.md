# JiVS Platform - Claude AI Implementation Guide

> **Quick Reference** | For detailed documentation, see **[docs/INDEX.md](docs/INDEX.md)**

---

## 🚨 CRITICAL: Continuous Testing Workflow

**⚠️ MANDATORY RULE**: ALWAYS run the continuous tester agent in the background before writing, modifying, or refactoring ANY code.

### Quick Start Continuous Testing:
```bash
# Start continuous monitoring (REQUIRED before coding)
bash scripts/continuous-tester.sh --watch > /tmp/continuous-watch.log 2>&1 &

# Verify it's running
ps aux | grep continuous-tester

# View real-time logs
tail -f /tmp/continuous-test-report.log
```

### Why This Matters:
- ✅ Prevents breaking changes from entering codebase
- ✅ Catches issues immediately (not hours later)
- ✅ Ensures seamless development with instant feedback
- ✅ Saves debugging time by catching errors early

**📖 Full Workflow Rules**: [.claude/WORKFLOW_RULES.md](.claude/WORKFLOW_RULES.md)

---

## 📋 Overview

JiVS (Java Integrated Virtualization System) is an enterprise-grade data integration, migration, and governance platform built with Claude AI assistance.

**Key Capabilities**:
- 🔄 Data Extraction & Migration
- ✅ Data Quality Management
- 🔒 GDPR/CCPA Compliance
- 📊 Analytics & Reporting
- 🤖 AI-Assisted Development

---

## 🚀 Quick Start

### Prerequisites
- Java 21
- Node.js 18+
- PostgreSQL 15
- Redis 7
- Docker (optional)

### Local Development

```bash
# Start backend
cd backend
mvn spring-boot:run

# Start frontend (in new terminal)
cd frontend
npm install
npm run dev
```

**Access**:
- Backend API: http://localhost:8080
- Frontend: http://localhost:3001
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

**Test Credentials**:
- Username: `admin`
- Password: `password`

### Docker Compose

```bash
docker-compose up -d
```

---

## 📁 Project Structure

```
jivs-platform/
├── backend/                # Spring Boot 3.2 + Java 21
│   ├── src/main/java/      # Application code
│   ├── src/main/resources/ # Config & migrations
│   └── src/test/           # Tests (130+)
├── frontend/               # React 18 + TypeScript
│   ├── src/components/     # Reusable components
│   ├── src/pages/          # Page components
│   ├── src/services/       # API services
│   └── tests/              # Playwright E2E (64 tests)
├── docs/                   # **Documentation hub**
│   ├── INDEX.md           # **Start here!**
│   ├── agents/            # AI agent docs
│   ├── testing/           # Test strategies
│   ├── operations/        # Runbooks, DR plans
│   ├── implementation/    # Status reports
│   ├── architecture/      # Design docs
│   └── archive/           # Historical docs
├── kubernetes/            # K8s deployments
├── scripts/               # Automation scripts
├── .claude/               # Claude Code agents
└── docker-compose.yml     # Local dev environment
```

---

## 💡 Technology Stack

### Backend
| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2 (Java 21) |
| Database | PostgreSQL 15 + Flyway |
| Caching | Redis 7 |
| Search | Elasticsearch 8 |
| Messaging | RabbitMQ |
| Security | Spring Security + JWT |
| API Docs | OpenAPI 3.0 (Swagger) |

### Frontend
| Component | Technology |
|-----------|------------|
| Framework | React 18 + TypeScript |
| State | Redux Toolkit |
| UI Library | Material-UI 5 |
| Routing | React Router 6 |
| HTTP Client | Axios |
| Charts | Recharts |
| Build | Vite |

---

## 🔑 Core Features

| Feature | Description | Documentation |
|---------|-------------|---------------|
| **Data Extraction** | Multi-source connectors (JDBC, SAP, File, API) | `backend/service/extraction/` |
| **Migration** | 7-phase migration lifecycle with rollback | `backend/service/migration/` |
| **Data Quality** | 6 dimensions, automated profiling | `backend/service/quality/` |
| **Compliance** | GDPR/CCPA requests, PII detection | `backend/service/compliance/` |
| **Retention** | Policy-based retention with 6 actions | `backend/service/retention/` |
| **Notifications** | Multi-channel (Email, SMS, Slack, Teams) | `backend/service/notification/` |
| **Storage** | Multi-backend with AES-256-GCM encryption | `backend/service/storage/` |
| **Analytics** | Dashboards, reports, exports | `backend/service/analytics/` |

**For detailed feature documentation**: See [docs/implementation/](docs/implementation/)

---

## 🤖 Claude Code Agents (13 Agents)

JiVS includes specialized AI agents for the full development lifecycle:

| Category | Agents | Purpose |
|----------|--------|---------|
| **Engineering** (3) | backend-architect, devops-automator, frontend-developer | Design, deploy, build |
| **Testing** (4) | test-writer-fixer, api-tester, performance-benchmarker, test-results-analyzer | Quality assurance |
| **Compliance** (1) | compliance-checker | GDPR/CCPA validation |
| **Operations** (3) | analytics-reporter, infrastructure-maintainer, workflow-optimizer | Monitoring, ops |
| **Product** (2) | sprint-prioritizer, project-shipper | Planning, releases |

**Quick Start**:
```bash
# Example usage
/jivs-backend-architect "Design data retention API"
/jivs-test-writer-fixer "Write tests for extraction service"
/jivs-compliance-checker "Validate GDPR compliance"
```

**Full Agent Documentation**: [docs/agents/AGENTS_README.md](docs/agents/AGENTS_README.md)

---

## 📖 Documentation Quick Links

### 🎯 Essential Reading
- **[Documentation Index](docs/INDEX.md)** - Start here for all docs
- **[Architecture Overview](docs/architecture/ARCHITECTURE.md)** - System design deep dive
- **[API Implementation Status](docs/implementation/API_IMPLEMENTATION_STATUS.md)** - Endpoint coverage
- **[Testing Strategy](docs/testing/E2E_TESTING_STRATEGY.md)** - Test approach

### 🔧 Operations
- **[Operational Runbook](docs/operations/OPERATIONAL_RUNBOOK.md)** - Daily/weekly procedures
- **[Disaster Recovery](docs/operations/DISASTER_RECOVERY.md)** - DR procedures (RTO/RPO)
- **[Security Audit Checklist](docs/operations/SECURITY_AUDIT_CHECKLIST.md)** - 91 checkpoints

### 🤖 Development with AI
- **[Agents Quick Reference](docs/agents/AGENTS_QUICK_REFERENCE.md)** - Fast agent lookup
- **[Agent Customization](docs/agents/AGENT_CUSTOMIZATION_TEMPLATE.md)** - Create new agents

### 📋 Implementation Status
- **[Application Status](docs/implementation/APPLICATION_STATUS.md)** - Module completion
- **[Issues & Mitigation Plan](docs/implementation/ISSUES_AND_MITIGATION_PLAN.md)** - Known issues
- **[Next Priorities](docs/implementation/NEXT_PRIORITIES.md)** - Roadmap

---

## 🔒 Security Highlights

✅ **Production-Ready Security**:
- JWT token blacklisting (Redis-based)
- Rate limiting (Resilience4j)
- SQL injection protection (15+ pattern detection)
- XSS protection (CSP headers + sanitization)
- Password policies (NIST 800-63B)
- AES-256-GCM encryption at rest
- TLS/SSL everywhere

**Details**: [docs/operations/SECURITY_AUDIT_CHECKLIST.md](docs/operations/SECURITY_AUDIT_CHECKLIST.md)

---

## 🚀 Deployment Options

### Local Development
```bash
mvn spring-boot:run  # Backend (port 8080)
npm run dev          # Frontend (port 3001)
```

### Docker Compose
```bash
docker-compose up -d  # All services
```

### Kubernetes (Production)
```bash
kubectl apply -f kubernetes/
# See: docs/operations/OPERATIONAL_RUNBOOK.md
```

**Full Deployment Guide**: [docs/operations/OPERATIONAL_RUNBOOK.md#deployment](docs/operations/OPERATIONAL_RUNBOOK.md)

---

## 🧪 Testing

### Backend Tests (130+ tests)
```bash
cd backend
mvn test
```

### Frontend Tests (64 E2E tests)
```bash
cd frontend
npx playwright test
```

**Test Coverage**: >80% overall
**Test Documentation**: [docs/testing/TESTING_SUMMARY.md](docs/testing/TESTING_SUMMARY.md)

---

## 📊 Performance

**Load Test Results** (100 concurrent users, 10 min):
| Metric | Value |
|--------|-------|
| Throughput | ~1000 req/sec |
| Avg Response | 85ms |
| p95 Response | 320ms |
| p99 Response | 650ms |
| Error Rate | 0.02% |

**Benchmarks**: [docs/implementation/APPLICATION_STATUS.md#performance](docs/implementation/APPLICATION_STATUS.md)

---

## 🐛 Troubleshooting

### Common Issues

**Backend won't start**:
```bash
# Check PostgreSQL
psql -U jivs_user -d jivs -h localhost

# Check Redis
redis-cli ping
```

**Frontend build fails**:
```bash
rm -rf node_modules
npm install
npm run dev
```

**API returns 401**:
- Clear browser localStorage
- Login again
- Check JWT token in localStorage

**Database connection errors**:
- Verify PostgreSQL running: `pg_isready`
- Check credentials in `backend/src/main/resources/application.yml`

**More troubleshooting**: [docs/operations/OPERATIONAL_RUNBOOK.md#troubleshooting](docs/operations/OPERATIONAL_RUNBOOK.md)

---

## 📝 Development Workflow

### Feature Development
1. **Plan**: Use `/jivs-sprint-prioritizer`
2. **Design**: Use `/jivs-backend-architect` + `/jivs-frontend-developer`
3. **Implement**: Write code
4. **Test**: Use `/jivs-test-writer-fixer`
5. **Review**: Code review + `/jivs-compliance-checker`
6. **Deploy**: Use `/jivs-devops-automator`

### Git Workflow
```bash
git checkout -b feature/your-feature
# Make changes
git add .
git commit -m "feat: your feature description"
git push origin feature/your-feature
# Create PR
```

---

## 🔗 Important Links

### Live Services
- Backend API: http://localhost:8080
- Frontend: http://localhost:3001
- Swagger UI: http://localhost:8080/swagger-ui.html

### Documentation
- **Main Index**: [docs/INDEX.md](docs/INDEX.md)
- **Architecture**: [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md)
- **Agents**: [docs/agents/AGENTS_README.md](docs/agents/AGENTS_README.md)

### External
- **GitHub**: https://github.com/cm65/newJivs
- **Claude Code Docs**: https://docs.claude.com/en/docs/claude-code

---

## ✅ Production Readiness

| Category | Status | Documentation |
|----------|--------|---------------|
| Security | ✅ Ready | [Security Audit](docs/operations/SECURITY_AUDIT_CHECKLIST.md) |
| High Availability | ✅ Ready | [K8s Config](kubernetes/) |
| Monitoring | ✅ Ready | [Operational Runbook](docs/operations/OPERATIONAL_RUNBOOK.md) |
| Backup & DR | ✅ Ready | [Disaster Recovery](docs/operations/DISASTER_RECOVERY.md) |
| CI/CD | ✅ Ready | [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml) |
| Documentation | ✅ Ready | [docs/INDEX.md](docs/INDEX.md) |

---

## 📜 Key Configuration

### Application Properties
```yaml
# backend/src/main/resources/application.yml
server.port: 8080
spring.datasource.url: jdbc:postgresql://localhost:5432/jivs
spring.redis.host: localhost
jivs.security.jwt.expiration: 3600000
```

### Environment Variables
```bash
DB_PASSWORD=<your-db-password>
JWT_SECRET=<your-jwt-secret>
ENCRYPTION_KEY=<your-encryption-key>
```

**Generate Secrets**: Use `scripts/generate-secrets.sh`

---

## 🎓 Learning Resources

### New Developers
1. Read [README.md](README.md) - Project overview
2. Browse [docs/INDEX.md](docs/INDEX.md) - Documentation hub
3. Review [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) - System design
4. Try agents: [docs/agents/AGENTS_README.md](docs/agents/AGENTS_README.md)

### Operations Team
1. [Operational Runbook](docs/operations/OPERATIONAL_RUNBOOK.md) - Daily ops
2. [Disaster Recovery](docs/operations/DISASTER_RECOVERY.md) - Emergency procedures
3. [Security Audit](docs/operations/SECURITY_AUDIT_CHECKLIST.md) - Security checks

### Product/Planning
1. [Sprint Prioritizer](docs/agents/AGENTS_README.md#jivs-sprint-prioritizer) - Planning
2. [Next Priorities](docs/implementation/NEXT_PRIORITIES.md) - Roadmap
3. [Project Shipper](docs/agents/AGENTS_README.md#jivs-project-shipper) - Releases

---

## 📞 Support

**For issues**:
1. Check [docs/INDEX.md](docs/INDEX.md) for relevant documentation
2. Review logs: `backend/logs/` or `kubectl logs`
3. Check [Troubleshooting Guide](docs/operations/OPERATIONAL_RUNBOOK.md#troubleshooting)
4. Create GitHub issue: https://github.com/cm65/newJivs/issues

**For questions**:
- Architecture: See [docs/architecture/](docs/architecture/)
- Operations: See [docs/operations/](docs/operations/)
- Testing: See [docs/testing/](docs/testing/)

---

## 📄 License

Proprietary - All rights reserved

---

## 🙏 Acknowledgments

Built with **Claude AI** (Anthropic) assistance:
- Architecture design
- Code implementation
- Testing strategies
- Documentation
- 13 specialized development agents

---

**Last Updated**: January 13, 2025
**Version**: 1.0.1
**Status**: ✅ Production-Ready

**📖 For detailed documentation, start here: [docs/INDEX.md](docs/INDEX.md)**
