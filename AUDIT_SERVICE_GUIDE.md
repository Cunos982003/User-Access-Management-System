# 📊 Audit Service - Complete Implementation Guide

## 🎯 Overview

The audit-service is a dedicated microservice for logging and monitoring all system events. It provides:
- **Event logging** - Track all user actions (login, register, password changes)
- **Search & filtering** - Query audit logs with multiple filters
- **CSV export** - Download audit logs for compliance
- **Metrics dashboard** - View statistics and failure rates

---

## 🏗️ Architecture

### Service Components

```
audit-service/
├── entity/
│   └── AuditLog.java          # Database entity
├── repository/
│   └── AuditLogRepository.java # Data access with custom queries
├── service/
│   ├── AuditService.java       # Business logic
│   └── CsvExportService.java   # CSV export functionality
├── controller/
│   └── AuditController.java    # REST API endpoints
├── dto/
│   ├── CreateAuditLogRequest.java
│   ├── AuditSearchRequest.java
│   ├── AuditLogResponse.java
│   ├── AuditMetricsResponse.java
│   └── ApiResponse.java
└── config/
    ├── OpenApiConfig.java      # Swagger configuration
    └── SecurityConfig.java     # Security settings
```

---

## 🗄️ Database Schema

```sql
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    user_id         UUID,
    username        VARCHAR(50),
    action          VARCHAR(50) NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     UUID,
    description     VARCHAR(500),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    status          VARCHAR(20) NOT NULL,  -- SUCCESS, FAILURE
    error_message   VARCHAR(1000),
    timestamp       TIMESTAMP NOT NULL,
    duration_ms     BIGINT,
    request_method  VARCHAR(10),
    request_path    VARCHAR(500)
);
```

**Indexes:** user_id, action, resource_type, timestamp, ip_address, status

---

## 🔌 API Endpoints

### 1. Create Audit Log
```http
POST /api/v1/audit/log
Content-Type: application/json

{
  "userId": "uuid",
  "username": "johndoe",
  "action": "USER_LOGIN",
  "resourceType": "USER",
  "description": "User logged in successfully",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "status": "SUCCESS"
}
```

**Response:** 200 OK - Audit log queued (async processing)

---

### 2. Search Audit Logs
```http
GET /api/v1/audit/search?userId=uuid&action=USER_LOGIN&page=0&size=20
Authorization: Bearer <token>
```

**Query Parameters:**
- `userId` - Filter by user ID
- `action` - Filter by action type
- `resourceType` - Filter by resource type
- `status` - Filter by status (SUCCESS/FAILURE)
- `startDate` - Start date (ISO 8601)
- `endDate` - End date (ISO 8601)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sortBy` - Sort field (default: timestamp)
- `sortDirection` - ASC or DESC (default: DESC)

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Success",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

---

### 3. Export to CSV
```http
GET /api/v1/audit/export/csv
Authorization: Bearer <token>
```

**Response:** CSV file download
```csv
ID,User ID,Username,Action,Resource Type,...
uuid1,uuid2,johndoe,USER_LOGIN,USER,...
```

---

### 4. Get Metrics
```http
GET /api/v1/audit/metrics
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalLogs": 1500,
    "successCount": 1450,
    "failureCount": 50,
    "failureRate": 3.33,
    "actionStatistics": {
      "USER_LOGIN": 500,
      "USER_REGISTER": 200,
      "PASSWORD_CHANGE": 100
    },
    "period": "all-time"
  }
}
```

---

## 🔗 Integration with Auth-Service

### AuditLogService.java (in auth-service)

```java
@Service
public class AuditLogService {
    
    @Async
    public void logSuccess(String action, UUID userId, String username, 
                          String description, String ipAddress, String userAgent) {
        // Sends HTTP POST to audit-service
        // Non-blocking, fire-and-forget
    }
    
    @Async
    public void logFailure(String action, String username, 
                          String description, String errorMessage, 
                          String ipAddress, String userAgent) {
        // Logs failure events
    }
}
```

### Usage in AuthService.java

```java
// After successful registration
auditLogService.logSuccess("USER_REGISTER", user.getId(), user.getUsername(),
    "New user registered", null, null);

// After successful login
auditLogService.logSuccess("USER_LOGIN", user.getId(), user.getUsername(),
    "User logged in successfully", null, null);

// After email verification
auditLogService.logSuccess("EMAIL_VERIFIED", user.getId(), user.getUsername(),
    "Email verified and account activated", null, null);
```

---

## 🚀 Running the Audit Service

### Option 1: Using Maven
```bash
# Run audit-service (port 8082)
./mvnw spring-boot:run -pl audit-service

# Run auth-service (port 8080)
./mvnw spring-boot:run -pl auth-service
```

### Option 2: Using IntelliJ IDEA
1. Open `AuditServiceApplication.java`
2. Right-click → Run
3. Service starts on port 8082

---

## 🧪 Testing Guide

### Step 1: Start Services
```bash
# Terminal 1: Start audit-service
./mvnw spring-boot:run -pl audit-service

# Terminal 2: Start auth-service
./mvnw spring-boot:run -pl auth-service
```

### Step 2: Open Swagger UI
- **Audit Service:** http://localhost:8082/api/v1/swagger-ui.html
- **Auth Service:** http://localhost:8080/api/v1/swagger-ui.html

### Step 3: Generate Audit Logs
1. Register a new user via auth-service
2. Verify email with OTP
3. Login with credentials
4. Check audit logs were created

### Step 4: Query Audit Logs
```bash
# Get all audit logs
curl http://localhost:8082/api/v1/audit/search

# Filter by action
curl http://localhost:8082/api/v1/audit/search?action=USER_LOGIN

# Get metrics
curl http://localhost:8082/api/v1/audit/metrics
```

---

## 📊 Common Audit Actions

| Action | Description | Triggered By |
|--------|-------------|--------------|
| `USER_REGISTER` | New user registration | AuthService.register() |
| `EMAIL_VERIFIED` | Email verification completed | AuthService.verifyOtp() |
| `USER_LOGIN` | Successful login | AuthService.login() |
| `USER_LOGOUT` | User logout | AuthService.logout() |
| `PASSWORD_CHANGE` | Password changed | AuthService.changePassword() |
| `PASSWORD_RESET` | Password reset | AuthService.resetPassword() |
| `TOKEN_REFRESH` | Access token refreshed | AuthService.refreshToken() |

---

## 🔍 Database Queries

```sql
-- View all audit logs
SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 20;

-- Count logs by action
SELECT action, COUNT(*) 
FROM audit_logs 
GROUP BY action 
ORDER BY COUNT(*) DESC;

-- View failed operations
SELECT * FROM audit_logs 
WHERE status = 'FAILURE' 
ORDER BY timestamp DESC;

-- View user activity
SELECT * FROM audit_logs 
WHERE user_id = 'your-user-uuid' 
ORDER BY timestamp DESC;

-- Daily activity
SELECT DATE(timestamp) as date, COUNT(*) as count
FROM audit_logs
GROUP BY DATE(timestamp)
ORDER BY date DESC;
```

---

## ⚙️ Configuration

### application.yaml
```yaml
server:
  port: 8082
  servlet:
    context-path: /api/v1
```

### application-dev.yaml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uam_dev
    username: postgres
    password: 123456
```

### Auth-service configuration
```yaml
audit:
  service:
    url: http://localhost:8082/api/v1
```

---

## 🎯 Key Features

✅ **Async logging** - Non-blocking, no performance impact on auth-service  
✅ **Flexible search** - Filter by user, action, date range, status  
✅ **CSV export** - Download logs for compliance and analysis  
✅ **Metrics dashboard** - Real-time statistics and failure rates  
✅ **Auto-retry** - Failed logs don't break auth operations  
✅ **Indexed queries** - Fast search on large datasets  
✅ **RESTful API** - Easy integration with other services  

---

## 📝 Next Steps

1. ✅ **Audit-service is complete**
2. ⏳ Add Redis caching for metrics
3. ⏳ Add time-based retention policies
4. ⏳ Add real-time alerting for failure spikes
5. ⏳ Add Grafana dashboard integration
6. ⏳ Add audit log replay functionality

---

**All audit service code is ready for testing!** 🎉
