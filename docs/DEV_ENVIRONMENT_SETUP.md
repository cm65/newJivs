# JiVS Platform - Development Environment Setup Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [IDE Configuration](#ide-configuration)
4. [Docker Development Environment](#docker-development-environment)
5. [Local Development (Without Docker)](#local-development-without-docker)
6. [Debugging](#debugging)
7. [Troubleshooting](#troubleshooting)
8. [Daily Workflow](#daily-workflow)

---

## Prerequisites

### Required Software

| Software | Version | Download Link | Purpose |
|----------|---------|---------------|---------|
| **Java JDK** | 21 | [Adoptium Temurin](https://adoptium.net/) | Backend runtime |
| **Node.js** | 18+ LTS | [NodeJS.org](https://nodejs.org/) | Frontend runtime |
| **Maven** | 3.9+ | [Maven.apache.org](https://maven.apache.org/) | Build tool (or use wrapper) |
| **Docker** | 24+ | [Docker.com](https://www.docker.com/) | Containerization |
| **Docker Compose** | 2.20+ | Included with Docker Desktop | Multi-container orchestration |
| **Git** | 2.40+ | [Git-scm.com](https://git-scm.com/) | Version control |
| **PostgreSQL** | 15+ | [PostgreSQL.org](https://www.postgresql.org/) | Database (local or Docker) |
| **Redis** | 7+ | [Redis.io](https://redis.io/) | Cache (local or Docker) |

### IDE Recommendations

**Primary (Choose One)**:
- **IntelliJ IDEA Ultimate** 2024.1+ (Recommended for Java development)
- **Visual Studio Code** 1.85+ with extensions (Lightweight, great for full-stack)

**Extensions for VS Code**:
```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-spring-boot
code --install-extension vscjava.vscode-spring-initializr
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension ms-playwright.playwright
code --install-extension ms-azuretools.vscode-docker
code --install-extension redhat.vscode-yaml
code --install-extension eamodio.gitlens
```

---

## Initial Setup

### 1. Clone Repository

```bash
git clone https://github.com/cm65/newJivs.git jivs-platform
cd jivs-platform
```

### 2. Configure Environment Variables

Create `.env` file in project root:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jivs
DB_USER=jivs_user
DB_PASSWORD=jivs_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# Elasticsearch Configuration
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=jivs_user
RABBITMQ_PASSWORD=rabbitmq_password

# Security
JWT_SECRET=your-generated-jwt-secret
ENCRYPTION_KEY=your-generated-encryption-key

# MinIO (S3-compatible storage)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123

# Application
SPRING_PROFILES_ACTIVE=local
LOG_LEVEL=DEBUG
```

### 3. Generate Secrets

```bash
# Generate JWT secret (64 bytes base64)
openssl rand -base64 64

# Generate encryption key (32 bytes base64)
openssl rand -base64 32

# Update .env file with generated values
```

### 4. Backend Setup

```bash
cd backend

# Install dependencies (using Maven wrapper)
./mvnw clean install -DskipTests

# Create necessary directories
mkdir -p logs storage temp

# Verify installation
./mvnw dependency:tree
```

### 5. Frontend Setup

```bash
cd frontend

# Install dependencies
npm ci

# Verify installation
npm list --depth=0
```

---

## IDE Configuration

### IntelliJ IDEA Setup

#### 1. Import Project

1. Open IntelliJ IDEA
2. File → Open → Select `jivs-platform` directory
3. Wait for Maven/Gradle sync to complete

#### 2. Configure JDK

1. File → Project Structure → Project
2. SDK: Select Java 21 (or add if not present)
3. Language Level: 21 - Record patterns, pattern matching for switch

#### 3. Configure Run Configurations

Run configurations are pre-configured in `.idea/runConfigurations/`:

- **JiVS Backend**: Runs Spring Boot application locally
- **JiVS Remote Debug**: Attaches to Docker container on port 5005

To run:
1. Click dropdown next to Run button
2. Select "JiVS Backend"
3. Click Run (Shift+F10) or Debug (Shift+F9)

#### 4. Enable Annotation Processing

1. Settings → Build, Execution, Deployment → Compiler → Annotation Processors
2. ✅ Enable annotation processing

#### 5. Code Style

1. Settings → Editor → Code Style → Java
2. Import: `.idea/java-code-style.xml` (if exists)
3. Or use: Scheme → Google Java Style

### Visual Studio Code Setup

#### 1. Open Workspace

```bash
cd jivs-platform
code .
```

#### 2. Install Recommended Extensions

VS Code will prompt to install workspace extensions. Click "Install All".

Or manually:
```bash
# View recommended extensions
cat .vscode/extensions.json

# Install all at once (from Extensions panel)
```

#### 3. Configure Java

1. Open Command Palette (Cmd+Shift+P / Ctrl+Shift+P)
2. Type: "Java: Configure Java Runtime"
3. Set Java 21 path:
   - macOS: `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
   - Linux: `/usr/lib/jvm/java-21-openjdk-amd64`
   - Windows: `C:\Program Files\Eclipse Adoptium\jdk-21`

#### 4. Verify Configuration

```bash
# Check Java version
java -version  # Should show 21.x.x

# Check Node version
node -v  # Should show v18.x.x or higher

# Check Maven
mvn -v   # Should show 3.9.x or higher
```

---

## Docker Development Environment

### Quick Start (Recommended)

```bash
# Start all services
docker-compose -f docker-compose.local.yml up -d

# Check status
docker-compose -f docker-compose.local.yml ps

# View logs
docker-compose -f docker-compose.local.yml logs -f backend

# Stop all services
docker-compose -f docker-compose.local.yml down

# Clean up (including volumes)
docker-compose -f docker-compose.local.yml down -v
```

### Service Endpoints

| Service | URL | Credentials |
|---------|-----|-------------|
| **Backend API** | http://localhost:8080 | - |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | - |
| **Frontend** | http://localhost:3001 | admin / password |
| **PostgreSQL** | localhost:5432 | jivs_user / jivs_password |
| **Adminer** | http://localhost:8081 | - |
| **Redis** | localhost:6379 | redis_password |
| **Elasticsearch** | http://localhost:9200 | - |
| **RabbitMQ Management** | http://localhost:15672 | jivs_user / rabbitmq_password |
| **MinIO Console** | http://localhost:9001 | minioadmin / minioadmin123 |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **MailHog** | http://localhost:8025 | - |

### Individual Service Management

```bash
# Start only infrastructure services
docker-compose -f docker-compose.local.yml up -d postgres redis elasticsearch rabbitmq minio

# Start backend only
docker-compose -f docker-compose.local.yml up -d backend

# Start frontend only
docker-compose -f docker-compose.local.yml up -d frontend

# Restart a specific service
docker-compose -f docker-compose.local.yml restart backend

# View logs for specific service
docker-compose -f docker-compose.local.yml logs -f backend

# Execute command in container
docker-compose -f docker-compose.local.yml exec backend bash
docker-compose -f docker-compose.local.yml exec postgres psql -U jivs_user -d jivs
```

### Development Workflow with Docker

```bash
# Option 1: Full Docker (all services in containers)
docker-compose -f docker-compose.local.yml up -d

# Option 2: Hybrid (infrastructure in Docker, code locally)
# Start infrastructure
docker-compose -f docker-compose.local.yml up -d postgres redis elasticsearch rabbitmq minio

# Run backend locally (in separate terminal)
cd backend
./mvnw spring-boot:run

# Run frontend locally (in another terminal)
cd frontend
npm run dev
```

---

## Local Development (Without Docker)

### 1. Install Infrastructure Services Locally

#### PostgreSQL

**macOS (Homebrew)**:
```bash
brew install postgresql@15
brew services start postgresql@15

# Create database and user
createdb jivs
psql -c "CREATE USER jivs_user WITH PASSWORD 'jivs_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE jivs TO jivs_user;"
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install postgresql-15

# Create database
sudo -u postgres psql
CREATE DATABASE jivs;
CREATE USER jivs_user WITH PASSWORD 'jivs_password';
GRANT ALL PRIVILEGES ON DATABASE jivs TO jivs_user;
\q
```

#### Redis

**macOS**:
```bash
brew install redis
brew services start redis

# Set password
redis-cli
CONFIG SET requirepass "redis_password"
CONFIG REWRITE
```

**Linux**:
```bash
sudo apt install redis-server
sudo systemctl start redis
sudo systemctl enable redis
```

#### Elasticsearch

**macOS**:
```bash
brew install elasticsearch@8
brew services start elasticsearch@8
```

**Linux**:
```bash
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.11.0-linux-x86_64.tar.gz
tar -xzf elasticsearch-8.11.0-linux-x86_64.tar.gz
cd elasticsearch-8.11.0/
./bin/elasticsearch -d
```

#### RabbitMQ

**macOS**:
```bash
brew install rabbitmq
brew services start rabbitmq

# Enable management plugin
rabbitmq-plugins enable rabbitmq_management
```

**Linux**:
```bash
sudo apt install rabbitmq-server
sudo systemctl start rabbitmq-server
sudo rabbitmq-plugins enable rabbitmq_management
```

### 2. Run Backend

```bash
cd backend

# Option 1: Using Maven wrapper
./mvnw spring-boot:run

# Option 2: Using IDE
# Click Run in IntelliJ/VS Code

# Option 3: Build and run JAR
./mvnw clean package -DskipTests
java -jar target/jivs-platform-*.jar
```

### 3. Run Frontend

```bash
cd frontend

# Development server with hot reload
npm run dev

# Access at http://localhost:3001
```

### 4. Run Tests

```bash
# Backend tests
cd backend
./mvnw test

# Frontend tests
cd frontend
npm test

# E2E tests (requires backend running)
npm run test:e2e
```

---

## Debugging

### Backend Debugging

#### IntelliJ IDEA

1. Set breakpoints in code (click gutter)
2. Select "JiVS Backend" configuration
3. Click Debug button (Shift+F9)
4. Application will start in debug mode

#### VS Code

1. Set breakpoints (F9)
2. Run → Start Debugging (F5)
3. Select "JiVS Backend (Local)"

#### Remote Debugging (Docker)

**IntelliJ IDEA**:
1. Start backend container: `docker-compose up -d backend`
2. Select "JiVS Remote Debug" configuration
3. Click Debug button

**VS Code**:
1. Start backend container: `docker-compose up -d backend`
2. Press F5
3. Select "JiVS Remote Debug (Docker)"

### Frontend Debugging

#### Chrome DevTools

1. Start frontend: `npm run dev`
2. Open http://localhost:3001
3. Open DevTools (F12)
4. Sources tab → set breakpoints

#### VS Code

1. Set breakpoints in TypeScript files
2. Press F5
3. Select "JiVS Frontend (Chrome)"
4. Chrome will launch with debugger attached

### Database Debugging

```bash
# Connect to PostgreSQL
psql -h localhost -U jivs_user -d jivs

# Common queries
\dt                          -- List tables
\d+ documents                -- Describe documents table
SELECT * FROM documents;     -- Query documents

# Check migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

# Or use Adminer (if Docker running)
# http://localhost:8081
```

### Redis Debugging

```bash
# Connect to Redis
redis-cli -a redis_password

# Common commands
KEYS *                       -- List all keys
GET key_name                 -- Get value
MONITOR                      -- Watch all commands
FLUSHALL                     -- Clear all data (dangerous!)
```

---

## Troubleshooting

### Common Issues

#### Backend Won't Start

**Error: Port 8080 already in use**
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
./mvnw spring-boot:run -Dserver.port=8081
```

**Error: Connection to database refused**
```bash
# Check PostgreSQL is running
pg_isready -h localhost -U jivs_user

# If not running (macOS)
brew services start postgresql@15

# If not running (Linux)
sudo systemctl start postgresql
```

**Error: Redis connection failed**
```bash
# Check Redis is running
redis-cli ping

# Expected: PONG

# If not running
brew services start redis       # macOS
sudo systemctl start redis      # Linux
```

#### Frontend Won't Start

**Error: npm ERR! code ENOENT**
```bash
# Clean install
rm -rf node_modules package-lock.json
npm install
```

**Error: Port 3001 already in use**
```bash
# Find and kill process
lsof -i :3001
kill -9 <PID>
```

#### Tests Failing

**Backend tests fail with "No tests found"**
```bash
# Clean and rebuild
./mvnw clean test
```

**E2E tests timeout**
```bash
# Ensure backend is running
curl http://localhost:8080/actuator/health

# Reinstall Playwright browsers
npx playwright install
```

#### Docker Issues

**Error: Cannot connect to Docker daemon**
```bash
# Start Docker Desktop (macOS/Windows)
# Or Docker service (Linux)
sudo systemctl start docker
```

**Error: Port conflicts**
```bash
# Check what's using ports
docker-compose -f docker-compose.local.yml down
lsof -i :5432 -i :6379 -i :9200

# Stop conflicting services
brew services stop postgresql  # macOS
sudo systemctl stop postgresql # Linux
```

**Services won't start / unhealthy**
```bash
# Check logs
docker-compose -f docker-compose.local.yml logs <service-name>

# Restart service
docker-compose -f docker-compose.local.yml restart <service-name>

# Nuclear option: clean restart
docker-compose -f docker-compose.local.yml down -v
docker-compose -f docker-compose.local.yml up -d
```

### Performance Issues

**Backend slow to start**
- Increase Java heap: `JAVA_OPTS=-Xmx4G`
- Disable unused auto-configurations in `application.yml`

**Frontend slow hot reload**
- Exclude `node_modules` from IDE indexing
- Increase Node memory: `NODE_OPTIONS=--max_old_space_size=4096`

**Database queries slow**
```sql
-- Check for missing indexes
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE tablename = 'documents';

-- Run ANALYZE
ANALYZE documents;
```

---

## Daily Workflow

### Morning Setup (Docker)

```bash
# Start infrastructure
docker-compose -f docker-compose.local.yml up -d postgres redis elasticsearch rabbitmq

# Verify services
docker-compose -f docker-compose.local.yml ps

# Start backend (local)
cd backend && ./mvnw spring-boot:run

# Start frontend (local, in new terminal)
cd frontend && npm run dev
```

### During Development

```bash
# Run tests continuously
npm run test:watch                    # Frontend unit tests
./mvnw test -Dtest=DocumentService    # Specific backend test

# Check code quality
./mvnw spotless:check                 # Backend formatting
npm run lint                          # Frontend linting

# Database migrations
./mvnw flyway:info                    # Check migration status
./mvnw flyway:migrate                 # Apply migrations
```

### Before Committing

```bash
# Run full test suite
cd backend && ./mvnw verify
cd frontend && npm test && npm run test:e2e

# Check build
./mvnw clean install                  # Backend
npm run build                         # Frontend

# Format code
./mvnw spotless:apply                 # Backend
npm run format                        # Frontend
```

### End of Day

```bash
# Commit work
git add .
git commit -m "feat: description of changes"
git push origin feature-branch

# Stop services (Docker)
docker-compose -f docker-compose.local.yml down

# Or keep running for tomorrow
docker-compose -f docker-compose.local.yml stop
```

---

## Additional Resources

### Documentation
- [Architecture Overview](architecture/ARCHITECTURE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Testing Strategy](testing/E2E_TESTING_STRATEGY.md)
- [Agents Guide](agents/AGENTS_README.md)

### Monitoring
- **Application Metrics**: http://localhost:8080/actuator/metrics
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

### Useful Commands

```bash
# View all Docker services
docker-compose -f docker-compose.local.yml ps

# View all logs
docker-compose -f docker-compose.local.yml logs -f

# Access PostgreSQL
docker-compose -f docker-compose.local.yml exec postgres psql -U jivs_user -d jivs

# Access Redis
docker-compose -f docker-compose.local.yml exec redis redis-cli -a redis_password

# Clean Docker
docker system prune -a --volumes

# Generate API documentation
./mvnw swagger2markup:convertSwagger2markup
```

---

**Last Updated**: January 2025
**Maintained By**: JiVS Platform Team
**Questions?**: Create an issue on GitHub
