# JiVS Information Management Platform

A comprehensive enterprise data management platform for data migration, application retirement, and information lifecycle management.

## Overview

JiVS IMP is a Java-based platform that enables:
- Any-to-any data migration (SAP ↔ non-SAP)
- Legacy system retirement
- GDPR/CCPA compliance
- Data quality management
- Document archiving
- Retention management

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed system architecture.

## Project Structure

```
jivs-platform/
├── backend/              # Spring Boot backend
├── frontend/             # React frontend
├── docker/               # Docker configurations
├── kubernetes/           # Kubernetes manifests
├── scripts/              # Utility scripts
├── docs/                 # Documentation
└── tests/                # Integration tests
```

## Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 15+
- Redis 7+
- Docker & Docker Compose
- Kubernetes (for production)

## Quick Start

### Using Docker Compose

```bash
# Clone the repository
git clone <repository-url>
cd jivs-platform

# Start all services
docker-compose up -d

# Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
```

### Manual Setup

#### Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Configuration

### Environment Variables

Create `.env` files in backend and frontend directories:

**Backend (.env)**
```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/jivs
DATABASE_USERNAME=jivs
DATABASE_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=your_secret_key
ENCRYPTION_KEY=your_encryption_key
```

**Frontend (.env)**
```properties
VITE_API_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws
```

## Features

### Core Features
- ✅ Data Extraction Engine (SAP, Oracle, SQL Server, MySQL, PostgreSQL)
- ✅ Business Object Framework (2000+ pre-defined objects)
- ✅ Data Transformation Engine
- ✅ Data Quality Tools
- ✅ Retention Management
- ✅ Migration Orchestration
- ✅ Document Archiving
- ✅ Search & Analytics
- ✅ RBAC & Security
- ✅ Audit Logging

### Compliance
- GDPR compliance
- CCPA compliance
- ISO 27001 certified architecture
- ISO 27017 cloud security

## API Documentation

API documentation is available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## Development

### Backend Development

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend Development

```bash
cd frontend
npm run dev
```

### Running Tests

```bash
# Backend tests
cd backend
./mvnw test

# Frontend tests
cd frontend
npm test
```

## Deployment

### Docker Deployment

```bash
# Build images
docker-compose build

# Deploy
docker-compose up -d
```

### Kubernetes Deployment

```bash
# Apply configurations
kubectl apply -f kubernetes/

# Check status
kubectl get pods -n jivs
```

### Cloud Deployments

See cloud-specific documentation:
- [AWS Deployment](./docs/deployment/aws.md)
- [Azure Deployment](./docs/deployment/azure.md)
- [GCP Deployment](./docs/deployment/gcp.md)

## Performance

- Data transfer rate: 30+ TB/day
- API response time: < 200ms (p95)
- Concurrent users: 10,000+
- Search response time: < 100ms

## Security

- TLS 1.3 encryption
- AES-256 data encryption
- OAuth 2.0 / JWT authentication
- Role-based access control
- Comprehensive audit logging

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for contribution guidelines.

## License

Proprietary - All rights reserved

## Support

- Documentation: [./docs](./docs)
- Issues: [GitHub Issues]
- Email: support@jivs-platform.com

## Roadmap

See [ROADMAP.md](./ROADMAP.md) for upcoming features and improvements.

## Authors

JiVS Platform Development Team

---

**Version:** 1.0.0
**Last Updated:** 2025-10-11
