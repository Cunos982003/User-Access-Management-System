# 🚀 Getting Started with UAM System

This guide will help you set up and run the User Access Management System locally.

## 📋 Prerequisites

- **Java 17+** ([Download](https://adoptium.net/))
- **Docker & Docker Compose** ([Download](https://www.docker.com/get-started))
- **IntelliJ IDEA** (recommended) or any Java IDE
- **Git**
- **Maven** (included via Maven Wrapper)

## 🏃 Quick Start with Docker Compose

The fastest way to run the entire system:

```bash
# 1. Clone the repository
git clone https://github.com/your-username/user-access-management-system.git
cd user-access-management-system

# 2. Start all services with Docker Compose
docker-compose up -d

# 3. Wait for services to be healthy (30-60 seconds)
docker-compose ps

# 4. Access the application
# API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

## 💻 Local Development Setup

### Step 1: Start Infrastructure Services

Start only PostgreSQL and Redis:

```bash
docker-compose up -d postgres redis
```

### Step 2: Configure IDE (IntelliJ IDEA)

1. **Open Project**: `File → Open → Select project root directory`
2. **Maven Import**: IntelliJ will auto-detect and import Maven modules
3. **Set Active Profile**: 
   - `Run → Edit Configurations`
   - Add VM Options: `-Dspring.profiles.active=dev`
4. **Configure Mail** (Optional for OTP testing):
   - Edit `auth-service/src/main/resources/application-dev.yaml`
   - Update mail settings with your SMTP credentials

### Step 3: Run the Application

**Option A: From IntelliJ**
- Navigate to `AuthServiceApplication.java`
- Right-click → `Run 'AuthServiceApplication'`

**Option B: From Command Line**
```bash
./mvnw spring-boot:run -pl auth-service
```

### Step 4: Verify Installation

Check application health:
```bash
curl http://localhost:8080/api/v1/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## 📚 API Documentation

Once the application is running, access interactive API docs:

- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v1/api-docs

## 🧪 Testing the Authentication Flow

### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "Password123!",
    "fullName": "John Doe",
    "phone": "+1234567890"
  }'
```

### 2. Verify Email with OTP

Check your email or application logs for the OTP code:

```bash
# Check logs for OTP
docker-compose logs auth-service | grep "OTP"

# Verify OTP
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "code": "123456"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "Password123!"
  }'
```

Response:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000-1234567890",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "...",
      "username": "johndoe",
      "email": "john@example.com",
      ...
    }
  }
}
```

### 4. Access Protected Endpoint

```bash
curl -X POST http://localhost:8080/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "currentPassword": "Password123!",
    "newPassword": "NewPassword123!"
  }'
```

## 🗄️ Database Access

**PostgreSQL Connection:**
- Host: `localhost`
- Port: `5432`
- Database: `uam_dev`
- Username: `postgres`
- Password: `postgres`

**Connect via psql:**
```bash
docker exec -it uam-postgres psql -U postgres -d uam_dev
```

**Useful queries:**
```sql
-- List all users
SELECT id, username, email, status FROM users;

-- Check roles and authorities
SELECT r.name as role, a.name as authority 
FROM roles r 
JOIN role_authorities ra ON r.id = ra.role_id
JOIN authorities a ON a.id = ra.authority_id;
```

## 📊 Monitoring & Observability

### Prometheus Metrics
- URL: http://localhost:9090
- Targets: http://localhost:9090/targets
- Query examples:
  - `http_server_requests_seconds_count`
  - `jvm_memory_used_bytes`

### Grafana Dashboards
- URL: http://localhost:3000
- Login: `admin` / `admin`
- Add Prometheus data source: `http://prometheus:9090`

### Spring Actuator
- Health: http://localhost:8080/api/v1/actuator/health
- Metrics: http://localhost:8080/api/v1/actuator/metrics
- Prometheus: http://localhost:8080/api/v1/actuator/prometheus

## 🔧 Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Mac/Linux

# Kill the process or change the port in application.yaml
```

### Database Connection Failed
```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Cannot Send Email / OTP
OTP codes are logged to console in dev mode:
```bash
# Check application logs
docker-compose logs -f auth-service | grep "OTP"
```

For testing, configure a test SMTP server:
- Use [Mailtrap](https://mailtrap.io/) for testing
- Update `application-dev.yaml` with Mailtrap credentials

### Maven Build Fails
```bash
# Clean and rebuild
./mvnw clean install -DskipTests

# If still failing, clear Maven cache
rm -rf ~/.m2/repository
./mvnw clean install
```

## 🧹 Cleanup

### Stop and remove all containers:
```bash
docker-compose down
```

### Remove volumes (deletes all data):
```bash
docker-compose down -v
```

## 📖 Next Steps

- [API Documentation](http://localhost:8080/api/v1/swagger-ui.html)
- [Architecture Overview](README.md#system-architecture)
- [Database Schema](README.md#database-design)
- [Security Design](README.md#security-design)
- [Contributing Guidelines](CONTRIBUTING.md)

## 💡 Tips

1. **Use Swagger UI** for interactive API testing
2. **Enable debug logging** for troubleshooting: Set `logging.level.com.r2s.uam=DEBUG`
3. **Watch logs in real-time**: `docker-compose logs -f auth-service`
4. **Hot reload** in IntelliJ: Enable "Build project automatically"
5. **Database migrations** are automatic via Flyway on startup

## 🆘 Getting Help

- Check [Build Status](BUILD_STATUS.md) for implementation status
- Review [Issues](https://github.com/your-username/uam/issues)
- Read the main [README](README.md)

---

Happy coding! 🎉
