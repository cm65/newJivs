# JiVS Platform - Startup Guide

**Date**: January 13, 2025
**Current Status**: Frontend running, backend needs dependencies

---

## Current System Status

### ✅ Running Services

1. **Frontend Dev Server**: http://localhost:3001 (HTTP 200 OK)
2. **Redis**: Running via Homebrew on port 6379

### ❌ Services Needed

1. **PostgreSQL**: Not installed or not running (required)
2. **Elasticsearch**: Not installed or not running (optional for full features)
3. **RabbitMQ**: Not installed or not running (optional for async messaging)
4. **Backend Application**: Waiting for PostgreSQL

---

## Quick Start Options

### Option 1: Using Docker (Recommended)

**Prerequisites**: Docker Desktop must be running

**Step 1: Start Docker Desktop**
```bash
# Open Docker Desktop application
# Or start from command line (if available)
open -a Docker
```

**Step 2: Wait for Docker to start** (check Docker icon in menu bar shows "Docker Desktop is running")

**Step 3: Start all backend services**
```bash
cd /Users/chandramahadevan/jivs-platform
docker-compose up -d
```

This will start:
- PostgreSQL on port 5432
- Redis on port 6379 (will use existing Homebrew Redis)
- Elasticsearch on port 9201
- RabbitMQ on ports 5672 (AMQP) and 15672 (Management UI)

**Step 4: Verify services are running**
```bash
docker-compose ps
```

**Step 5: Start backend**
```bash
cd backend
mvn spring-boot:run
```

**Step 6: Access application**
- Frontend: http://localhost:3001
- Backend API: http://localhost:8080
- RabbitMQ Management: http://localhost:15672 (jivs / jivs_password)

---

### Option 2: Install PostgreSQL via Homebrew

**Step 1: Install PostgreSQL**
```bash
brew install postgresql@15
```

**Step 2: Start PostgreSQL service**
```bash
brew services start postgresql@15
```

**Step 3: Create database and user**
```bash
# Connect to PostgreSQL
psql postgres

# In psql prompt:
CREATE DATABASE jivs;
CREATE USER jivs WITH PASSWORD 'jivs';
GRANT ALL PRIVILEGES ON DATABASE jivs TO jivs;
\q
```

**Step 4: Start backend**
```bash
cd backend
mvn spring-boot:run
```

**Note**: Without Elasticsearch and RabbitMQ, some features won't work:
- Full-text search (Elasticsearch)
- Async message processing (RabbitMQ)

But core features will work:
- Authentication
- Extractions
- Migrations
- Data Quality
- Compliance

---

### Option 3: Minimal Setup (PostgreSQL only via Docker)

If you only want to start PostgreSQL:

```bash
docker run -d \
  --name jivs-postgres \
  -e POSTGRES_DB=jivs \
  -e POSTGRES_USER=jivs \
  -e POSTGRES_PASSWORD=jivs \
  -p 5432:5432 \
  postgres:15
```

Then start backend:
```bash
cd backend
mvn spring-boot:run
```

---

## Starting Individual Services

### PostgreSQL (Docker)
```bash
docker run -d --name jivs-postgres \
  -e POSTGRES_DB=jivs \
  -e POSTGRES_USER=jivs \
  -e POSTGRES_PASSWORD=jivs \
  -p 5432:5432 \
  -v jivs_postgres_data:/var/lib/postgresql/data \
  postgres:15
```

### Redis (Already Running via Homebrew)
```bash
# Check status
brew services list | grep redis

# If not running:
brew services start redis

# Test connection
redis-cli ping  # Should return PONG
```

### Elasticsearch (Docker)
```bash
docker run -d --name jivs-elasticsearch \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -p 9201:9200 \
  -p 9300:9300 \
  -v jivs_elasticsearch_data:/usr/share/elasticsearch/data \
  elasticsearch:8.10.2
```

### RabbitMQ (Docker)
```bash
docker run -d --name jivs-rabbitmq \
  -e RABBITMQ_DEFAULT_USER=jivs \
  -e RABBITMQ_DEFAULT_PASS=jivs_password \
  -p 5672:5672 \
  -p 15672:15672 \
  -v jivs_rabbitmq_data:/var/lib/rabbitmq \
  rabbitmq:3-management-alpine
```

---

## Backend Application

### Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Backend Configuration
The backend is configured to connect to:
- **PostgreSQL**: localhost:5432, database=jivs, user=jivs, password=jivs
- **Redis**: localhost:6379 (no password)
- **Elasticsearch**: localhost:9201 (note: port 9201, not 9200)
- **RabbitMQ**: localhost:5672, user=jivs, password=jivs_password

### Check Backend Health
```bash
curl http://localhost:8080/actuator/health
```

Expected response when all dependencies are running:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

## Verification Steps

### 1. Check All Services
```bash
# PostgreSQL
psql -h localhost -U jivs -d jivs -c "SELECT version();"

# Redis
redis-cli ping

# Elasticsearch
curl http://localhost:9201

# RabbitMQ
curl -u jivs:jivs_password http://localhost:15672/api/overview

# Backend
curl http://localhost:8080/actuator/health

# Frontend
curl -I http://localhost:3001
```

### 2. Login to Application
1. Open browser: http://localhost:3001
2. You should see the login page
3. Login with credentials:
   - **Username**: `admin`
   - **Password**: `password`

### 3. Test Key Features
- [ ] Dashboard loads with statistics
- [ ] Navigate to Extractions page
- [ ] Navigate to Migrations page
- [ ] Create a test extraction
- [ ] Check WebSocket real-time updates

---

## Troubleshooting

### Issue: Docker is not running
**Solution**: Start Docker Desktop application

```bash
# Check if Docker is running
docker ps

# If error "Cannot connect to the Docker daemon":
# - Open Docker Desktop application
# - Wait for "Docker Desktop is running" in menu bar
# - Try again
```

### Issue: Port already in use
**Solutions**:

**PostgreSQL port 5432**:
```bash
# Find process using port 5432
lsof -ti:5432

# Kill the process
kill -9 $(lsof -ti:5432)

# Or stop existing PostgreSQL
brew services stop postgresql@15
```

**Redis port 6379** (already handled by Homebrew):
```bash
brew services restart redis
```

**Elasticsearch port 9201**:
```bash
lsof -ti:9201 | xargs kill -9
```

### Issue: Backend fails to start with "Connection refused"
**Cause**: PostgreSQL not running

**Solution**: Start PostgreSQL first (see options above)

### Issue: Backend starts but health check returns DOWN
**Cause**: One or more required services not running

**Solution**: Check logs for specific service failure
```bash
# Check backend logs
tail -100 backend/logs/jivs-platform.log | grep ERROR

# Or check console output for startup errors
```

### Issue: Flyway migration fails
**Cause**: Database schema issues

**Solutions**:
1. Check if database exists:
   ```bash
   psql -h localhost -U jivs -l | grep jivs
   ```

2. Reset database (WARNING: deletes all data):
   ```bash
   psql -h localhost -U jivs -c "DROP DATABASE jivs;"
   psql -h localhost -U jivs -c "CREATE DATABASE jivs;"
   ```

3. Restart backend (Flyway will auto-migrate)

### Issue: Cannot login - 401 Unauthorized
**Solutions**:
1. Check backend logs for JWT errors
2. Verify admin user exists:
   ```bash
   psql -h localhost -U jivs -d jivs -c "SELECT * FROM users WHERE username='admin';"
   ```

3. If no admin user, backend should create one on startup
4. Check JWT secret is configured (uses default in dev mode)

---

## Stopping Services

### Stop Docker Compose Services
```bash
docker-compose down
```

### Stop Individual Docker Containers
```bash
docker stop jivs-postgres jivs-redis jivs-elasticsearch jivs-rabbitmq
docker rm jivs-postgres jivs-redis jivs-elasticsearch jivs-rabbitmq
```

### Stop Homebrew Services
```bash
brew services stop redis
brew services stop postgresql@15
```

### Stop Backend
```bash
# Find Java process
ps aux | grep spring-boot

# Kill process
kill <PID>

# Or use Ctrl+C in terminal where mvn spring-boot:run is running
```

### Stop Frontend
```bash
# Find node process
lsof -ti:3001 | xargs kill -9

# Or use Ctrl+C in terminal where npm run dev is running
```

---

## Environment Variables (Optional)

Create a `.env` file in the project root for custom configuration:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/jivs
DATABASE_USERNAME=jivs
DATABASE_PASSWORD=jivs

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Elasticsearch
ELASTICSEARCH_URIS=http://localhost:9201

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=jivs
RABBITMQ_PASSWORD=jivs_password

# Security
JWT_SECRET=your-very-long-secret-key-minimum-256-bits
ENCRYPTION_KEY=your-encryption-key-32-bytes

# Storage
STORAGE_PROVIDER=local
STORAGE_LOCAL_PATH=/tmp/jivs/storage
```

---

## Production Considerations

For production deployment:

1. **Use strong passwords**:
   - PostgreSQL password
   - Redis password (enable authentication)
   - RabbitMQ password
   - JWT secret (256+ bits)

2. **Enable security**:
   - Elasticsearch xpack.security
   - SSL/TLS for all services
   - Firewall rules

3. **Use persistent volumes**:
   - Database backups
   - Redis AOF/RDB persistence
   - Elasticsearch snapshots

4. **Monitor resources**:
   - Database connection pool
   - Memory usage (ES, RabbitMQ)
   - Disk space

5. **Use production profiles**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

---

## Quick Reference

### Service Ports
| Service       | Port(s)        | URL                        |
|---------------|----------------|----------------------------|
| Frontend      | 3001           | http://localhost:3001      |
| Backend       | 8080           | http://localhost:8080      |
| PostgreSQL    | 5432           | localhost:5432             |
| Redis         | 6379           | localhost:6379             |
| Elasticsearch | 9201, 9300     | http://localhost:9201      |
| RabbitMQ AMQP | 5672           | localhost:5672             |
| RabbitMQ UI   | 15672          | http://localhost:15672     |

### Default Credentials
| Service       | Username | Password       |
|---------------|----------|----------------|
| Application   | admin    | password       |
| PostgreSQL    | jivs     | jivs           |
| RabbitMQ      | jivs     | jivs_password  |
| Elasticsearch | -        | (disabled)     |
| Redis         | -        | (no password)  |

### Useful Commands
```bash
# Check all services
docker-compose ps
brew services list

# View logs
docker-compose logs -f
tail -f backend/logs/jivs-platform.log

# Restart services
docker-compose restart
brew services restart redis

# Clean up
docker-compose down -v  # Remove volumes
docker system prune -a  # Clean Docker cache
```

---

## Next Steps

Once all services are running:

1. **Verify Application**:
   - Login at http://localhost:3001
   - Test extraction creation
   - Test migration functionality
   - Check WebSocket real-time updates

2. **Run Tests**:
   ```bash
   # Backend tests
   cd backend && mvn test

   # Frontend tests
   cd frontend && npm test

   # E2E tests
   cd frontend && npx playwright test
   ```

3. **Continue Development**:
   - Implement dark mode UI (Sprint 3 Priority 1)
   - Clean up technical debt
   - Add advanced filtering
   - Performance optimization

---

## Support

**Documentation**:
- Main README: `/README.md`
- Architecture: `/ARCHITECTURE.md`
- Claude Guide: `/CLAUDE.md`
- UI Fix Summary: `/UI_FIX_SUMMARY.md`
- Next Priorities: `/NEXT_PRIORITIES.md`

**Logs**:
- Backend: `backend/logs/jivs-platform.log`
- Frontend: Browser console (F12)
- Docker: `docker-compose logs`

---

**Created**: January 13, 2025
**Status**: Ready for startup
**Frontend**: ✅ Running
**Backend**: ⏳ Waiting for PostgreSQL

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
