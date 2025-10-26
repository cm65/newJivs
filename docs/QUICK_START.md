# JiVS Platform - Quick Start Guide

Get the JiVS Platform running in 5 minutes!

## 🚀 Fastest Way: Docker Compose

### Prerequisites
- Docker Desktop installed ([Download here](https://www.docker.com/products/docker-desktop))
- Git installed

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/cm65/newJivs.git
cd newJivs

# 2. Start all services
docker-compose up -d

# 3. Wait for services to start (about 60 seconds)
docker-compose ps

# 4. Access the application
```

**🌐 Application URLs:**
- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html

**🔑 Default Credentials:**
- Username: `admin`
- Password: `password`

---

## 📦 What's Included

JiVS Platform includes:
- ✅ **Document Management**: Upload, archive, search documents
- ✅ **Data Extraction**: Multi-source data connectors
- ✅ **Migration Engine**: 7-phase migration lifecycle
- ✅ **Data Quality**: Automated profiling and validation
- ✅ **Compliance**: GDPR/CCPA request handling
- ✅ **Analytics**: Dashboards and reporting

---

## 🧪 Test the Document Management Feature

1. Login with credentials above
2. Click **"Documents"** in left sidebar
3. Click **"Upload Document"**
4. Upload a test file
5. Select the document and click **"Archive Selected"**
6. Use the search bar to find your document

---

## 🛠️ Development Setup (Without Docker)

### Backend (Java 21 + Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### Frontend (React + TypeScript)
```bash
cd frontend
npm install
npm run dev
```

---

## 📚 Documentation

- **Full Documentation**: [docs/INDEX.md](docs/INDEX.md)
- **Architecture**: [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md)
- **API Docs**: http://localhost:8080/swagger-ui.html (when running)
- **Operational Runbook**: [docs/operations/OPERATIONAL_RUNBOOK.md](docs/operations/OPERATIONAL_RUNBOOK.md)

---

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Check what's using the ports
lsof -i :3001  # Frontend
lsof -i :8080  # Backend
lsof -i :5432  # PostgreSQL

# Stop conflicting services or change ports in docker-compose.yml
```

### Database Connection Errors
```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Frontend Not Loading
```bash
# Rebuild frontend
docker-compose down
docker-compose up -d --build frontend
```

---

## 🚀 Deploy to Production

See deployment guides:
- **Heroku**: [docs/deployment/HEROKU.md](docs/deployment/HEROKU.md)
- **Kubernetes**: [kubernetes/README.md](kubernetes/README.md)
- **AWS/GCP/Azure**: [docs/operations/OPERATIONAL_RUNBOOK.md](docs/operations/OPERATIONAL_RUNBOOK.md)

---

## 📞 Support

- **Issues**: https://github.com/cm65/newJivs/issues
- **Documentation**: [docs/INDEX.md](docs/INDEX.md)

---

## 📄 License

Proprietary - All rights reserved

---

**Built with Claude AI** 🤖
