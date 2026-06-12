# 📧 Notification Service - Complete Guide

## 🎯 Overview

The notification-service handles all email communications in the UAM system. It provides:
- **Email sending** - Send emails using HTML templates
- **Template management** - Pre-built templates for OTP, Welcome, Password Reset
- **Email logging** - Track all sent emails with status
- **Delivery tracking** - Monitor success/failure rates

**Port:** 8081  
**Swagger UI:** http://localhost:8081/api/v1/swagger-ui.html

---

## 📦 Features

✅ **3 Email Templates:** OTP, Welcome, Password Reset  
✅ **Async Processing:** Non-blocking email sending  
✅ **Email Logging:** Track all emails in database  
✅ **Error Handling:** Automatic retry tracking  
✅ **HTML Templates:** Professional email designs  
✅ **RESTful API:** Easy integration with other services  

---

## 🔌 API Endpoints

### 1. Send Email
```http
POST /api/v1/notifications/send
Content-Type: application/json

{
  "recipient": "user@example.com",
  "subject": "Your OTP Code",
  "templateName": "OTP",
  "variables": {
    "otpCode": "123456",
    "purpose": "email verification"
  }
}
```

**Templates Available:**
- `OTP` - Variables: `otpCode`, `purpose`
- `WELCOME` - Variables: `username`
- `PASSWORD_RESET` - Variables: `otpCode`

### 2. Get Email Logs
```http
GET /api/v1/notifications/logs?page=0&size=20
Authorization: Bearer <token>
```

### 3. Get Logs by Recipient
```http
GET /api/v1/notifications/logs/recipient/user@example.com?page=0&size=20
Authorization: Bearer <token>
```

### 4. Get Logs by Status
```http
GET /api/v1/notifications/logs/status/SENT?page=0&size=20
Authorization: Bearer <token>
```

**Status Values:** PENDING, SENT, FAILED

---

## 🗄️ Database Schema

```sql
CREATE TABLE email_logs (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    retry_count INTEGER DEFAULT 0
);
```

---

## 🚀 Running the Service

```bash
# Start notification-service
./mvnw spring-boot:run -pl notification-service

# Or with IntelliJ
# Run NotificationServiceApplication.java
```

**Open Swagger UI:** http://localhost:8081/api/v1/swagger-ui.html

---

## 📊 Testing

1. **Send Test Email:**
```bash
curl -X POST http://localhost:8081/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "subject": "Test Email",
    "templateName": "OTP",
    "variables": {
      "otpCode": "123456",
      "purpose": "testing"
    }
  }'
```

2. **View Email Logs:**
```bash
curl http://localhost:8081/api/v1/notifications/logs
```

3. **Check Database:**
```sql
SELECT * FROM email_logs ORDER BY created_at DESC LIMIT 10;
```

---

**All notification-service code is ready!** 🎉

---

# 👥 User Service - Complete Guide

## 🎯 Overview

The user-service handles user profile management and admin operations. It provides:
- **Profile Management** - View and update own profile
- **Admin Operations** - Search, update, lock/unlock users
- **Status Management** - Enable/disable/lock accounts
- **User Search** - Filter by keyword and status

**Port:** 8083  
**Swagger UI:** http://localhost:8083/api/v1/swagger-ui.html

---

## 📦 Features

✅ **Profile CRUD:** View and update user profiles  
✅ **Admin Dashboard:** Search and manage all users  
✅ **Status Control:** Lock, unlock, enable, disable accounts  
✅ **Advanced Search:** Filter by keyword and status  
✅ **Role-Based Access:** Protected admin endpoints  
✅ **Input Validation:** Comprehensive request validation  

---

## 🔌 API Endpoints

### User Profile Endpoints

#### 1. Get Own Profile
```http
GET /api/v1/users/me
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "+1234567890",
    "avatarUrl": "https://...",
    "status": "ACTIVE",
    "roles": ["ROLE_USER"],
    "createdAt": "2024-01-15T10:30:00",
    "lastLogin": "2024-01-20T14:20:00"
  }
}
```

#### 2. Update Own Profile
```http
PUT /api/v1/users/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "John Doe Updated",
  "phone": "+9876543210",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

---

### Admin Endpoints (Require ADMIN Role)

#### 3. Search Users
```http
GET /api/v1/users?keyword=john&status=ACTIVE&page=0&size=20
Authorization: Bearer <admin-token>
```

**Query Parameters:**
- `keyword` - Search in username, email, fullName
- `status` - Filter by status (ACTIVE, LOCKED, DISABLED)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)

#### 4. Get User by ID
```http
GET /api/v1/users/{userId}
Authorization: Bearer <admin-token>
```

#### 5. Update User
```http
PUT /api/v1/users/{userId}
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "fullName": "Updated Name",
  "phone": "+1111111111"
}
```

#### 6. Update User Status
```http
PATCH /api/v1/users/{userId}/status
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "status": "LOCKED"
}
```

**Valid Status Values:** ACTIVE, LOCKED, DISABLED

---

## 🗄️ Database Schema

Uses existing `users` table from auth-service:
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);
```

---

## 🚀 Running the Service

```bash
# Start user-service
./mvnw spring-boot:run -pl user-service

# Or with IntelliJ
# Run UserServiceApplication.java
```

**Open Swagger UI:** http://localhost:8083/api/v1/swagger-ui.html

---

## 📊 Testing

### Test Profile Management
1. Login via auth-service to get JWT token
2. Get profile: `GET /users/me` with Bearer token
3. Update profile: `PUT /users/me` with Bearer token

### Test Admin Operations
1. Login with admin account
2. Search users: `GET /users?keyword=john`
3. Lock user: `PATCH /users/{id}/status` with `{"status": "LOCKED"}`
4. View user: `GET /users/{id}`

---

## 🔒 Security

- **JWT Authentication:** All endpoints require valid JWT token
- **Role-Based Access:** Admin endpoints check for ADMIN role
- **Status Validation:** Cannot set status to PENDING
- **Input Validation:** All requests validated with Jakarta Validation

---

**All user-service code is ready!** 🎉
