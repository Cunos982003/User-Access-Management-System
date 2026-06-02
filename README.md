# 🔐 User Access Management System (UAM)

> **A production-grade, enterprise-standard User Access Management module built for Fresher → Junior Java Backend Developers.**  
> Designed to be CV-ready, microservice-compatible, and security-first.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Module Breakdown](#module-breakdown)
- [Database Design](#database-design)
- [API Design](#api-design)
- [Security Design](#security-design)
- [Caching Strategy](#caching-strategy)
- [Logging & Monitoring](#logging--monitoring)
- [Environment Profiles](#environment-profiles)
- [Database Migration (Flyway)](#database-migration-flyway)
- [Docker & CI/CD](#docker--cicd)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Use Cases Summary](#use-cases-summary)

---

## Overview

**UAM** is a comprehensive User Access Management system that covers the full lifecycle of user identity: registration, authentication, authorization, profile management, security hardening, notifications, and auditing.

| Goal | Detail |
|------|--------|
| 🎯 Primary | Hands-on backend project for Fresher developers |
| 📄 CV-Ready | Demonstrates enterprise-grade Java Backend skills |
| 🔌 Reusable | Can be embedded as a microservice or shared library |
| 🏢 Enterprise-fit | Covers ~80% of real-world user management requirements |

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (JJWT) |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway |
| Cache (Local) | Caffeine |
| Cache (Distributed) | Redis |
| API Docs | Springdoc OpenAPI 3 (Swagger UI) |
| Logging | Logback (with structured JSON output) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Monitoring | Spring Actuator, Prometheus, Grafana |
| IDE | IntelliJ IDEA |

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway (optional)               │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
  ┌─────────────┐ ┌─────────────┐ ┌──────────────────┐
  │ auth-service│ │ user-service│ │notification-svc  │
  │  (UC01-10)  │ │  (UC11-17)  │ │   (UC24-26)      │
  └──────┬──────┘ └──────┬──────┘ └────────┬─────────┘
         │               │                 │
         └───────────────┼─────────────────┘
                         ▼
              ┌─────────────────────┐
              │    audit-service    │
              │     (UC27-31)       │
              └──────────┬──────────┘
                         │
         ┌───────────────┼──────────────────┐
         ▼               ▼                  ▼
   ┌──────────┐   ┌──────────┐      ┌───────────┐
   │PostgreSQL│   │  Redis   │      │Prometheus │
   └──────────┘   └──────────┘      │ /Grafana  │
                                    └───────────┘
```

### Communication Flow

```
Client
  │
  ├── POST /auth/register       → auth-service → DB (create PENDING user)
  │                                           → notification-service (send OTP)
  │
  ├── POST /auth/verify-otp     → auth-service → DB (set ACTIVE)
  │
  ├── POST /auth/login          → auth-service → DB (validate credentials)
  │                                           → return Access Token + Refresh Token
  │
  ├── GET  /users/me            → user-service (requires Bearer token)
  │
  └── Any action                → audit-service (async log)
```

---

## Module Breakdown

### 1. `auth-service` — Authentication & Authorization

Handles the entire identity lifecycle: registration, login, token management, and RBAC.

**Key Responsibilities:**
- User registration with `PENDING` status
- OTP-based email verification → `ACTIVE` status
- JWT Access Token + Refresh Token issuance
- Token refresh without re-login
- Logout with token invalidation
- Failed login attempt tracking + account locking
- Forgot/reset password via OTP
- In-session password change
- Role-based access control (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR`)
- Fine-grained authority control (`READ_USER`, `UPDATE_USER`, `DELETE_USER`)

---

### 2. `user-service` — User Profile Management

Manages user profiles and admin-level user operations.

**Key Responsibilities:**
- View and update personal profile (avatar, fullname, phone)
- Change email with re-verification
- Admin: search users by username / email / role / status
- Admin: update any user's info
- Admin: enable / disable / lock users
- Admin: reset user passwords

---

### 3. `notification-service` — Email / OTP

Handles all outbound communication (email, OTP codes).

**Key Responsibilities:**
- Send OTP on registration
- Send reset password OTP
- Admin-triggered bulk/forced email notifications

---

### 4. `audit-service` — Audit & Monitoring

Records all security-relevant events and exposes metrics.

**Key Responsibilities:**
- Log login success/failure events
- Log user actions: profile update, password change, admin actions
- Export audit logs as CSV
- Spring Actuator endpoints (`/health`, `/metrics`, `/readiness`, `/liveness`)
- Prometheus metrics integration for Grafana dashboards

---

## Database Design

### Core Tables

```sql
-- users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    phone         VARCHAR(20),
    avatar_url    VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | ACTIVE | LOCKED | DISABLED
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- roles
CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL  -- ROLE_USER | ROLE_ADMIN | ROLE_MODERATOR
);

-- authorities (fine-grained permissions)
CREATE TABLE authorities (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL  -- READ_USER | UPDATE_USER | DELETE_USER
);

-- user_roles (many-to-many)
CREATE TABLE user_roles (
    user_id UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- role_authorities (many-to-many)
CREATE TABLE role_authorities (
    role_id      INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    authority_id INTEGER NOT NULL REFERENCES authorities(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, authority_id)
);

-- refresh_tokens
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    device_id  UUID,
    expires_at TIMESTAMP   NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- otp_codes
CREATE TABLE otp_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code       VARCHAR(10) NOT NULL,
    type       VARCHAR(30) NOT NULL,  -- VERIFY_EMAIL | RESET_PASSWORD | CHANGE_EMAIL
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- login_attempts
CREATE TABLE login_attempts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID      REFERENCES users(id) ON DELETE CASCADE,
    username_try VARCHAR(100),
    ip_address   VARCHAR(45),
    success      BOOLEAN   NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- devices (Device Tracking)
CREATE TABLE devices (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(200),
    ip_address  VARCHAR(45),
    os          VARCHAR(100),
    browser     VARCHAR(100),
    last_seen   TIMESTAMP   NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE
);

-- audit_logs
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    target_id   UUID,
    action      VARCHAR(100) NOT NULL,  -- LOGIN_SUCCESS | UPDATE_PROFILE | ADMIN_LOCK_USER ...
    detail      JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Entity Relationship Diagram

```
users ──< user_roles >── roles ──< role_authorities >── authorities
  │
  ├──< refresh_tokens
  ├──< otp_codes
  ├──< login_attempts
  ├──< devices
  └──< audit_logs (actor / target)
```

---

## API Design

Base URL: `http://localhost:8080/api/v1`

### Auth Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | Public | Register new account |
| POST | `/auth/verify-otp` | Public | Verify OTP to activate account |
| POST | `/auth/login` | Public | Login, returns JWT |
| POST | `/auth/refresh-token` | Public | Refresh access token |
| POST | `/auth/logout` | Bearer | Logout and invalidate token |
| POST | `/auth/forgot-password` | Public | Send OTP to reset password |
| POST | `/auth/reset-password` | Public | Reset password using OTP |
| POST | `/auth/change-password` | Bearer | Change password (logged-in) |

### User Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/users/me` | Bearer | Get own profile |
| PUT | `/users/me` | Bearer | Update own profile |
| PUT | `/users/me/email` | Bearer | Request email change |
| GET | `/users` | ADMIN | Search & list all users |
| GET | `/users/{id}` | ADMIN | Get user by ID |
| PUT | `/users/{id}` | ADMIN | Update user info |
| PATCH | `/users/{id}/status` | ADMIN | Enable / disable / lock |
| POST | `/users/{id}/reset-password` | ADMIN | Admin resets user password |

### Audit Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/audit/logs` | ADMIN | Get paginated audit logs |
| GET | `/audit/logs/export` | ADMIN | Download audit CSV |

### Response Format

```json
{
  "success": true,
  "statusCode": 200,
  "message": "OK",
  "data": { ... },
  "timestamp": "2024-11-15T10:30:00Z"
}
```

### Error Response Format

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "Email is already in use" }
  ],
  "timestamp": "2024-11-15T10:30:00Z"
}
```

---

## Security Design

### JWT Token Strategy

```
Access Token:
  - Algorithm : HS256 (or RS256 for production)
  - Expiry    : 15 minutes
  - Claims    : sub (userId), roles, authorities, iat, exp

Refresh Token:
  - Stored in DB (refresh_tokens table)
  - Expiry    : 7 days
  - Rotated on each use (rotation strategy)
  - Invalidated on logout
```

### Role Hierarchy

```
ROLE_ADMIN
    └── ROLE_MODERATOR
            └── ROLE_USER
```

> A user with `ROLE_ADMIN` automatically inherits all permissions of `ROLE_MODERATOR` and `ROLE_USER`.

### Authority Matrix

| Authority | ROLE_USER | ROLE_MODERATOR | ROLE_ADMIN |
|-----------|-----------|----------------|------------|
| READ_USER | ✅ (own) | ✅ (all) | ✅ |
| UPDATE_USER | ✅ (own) | ❌ | ✅ |
| DELETE_USER | ❌ | ❌ | ✅ |
| LOCK_USER | ❌ | ✅ | ✅ |
| EXPORT_AUDIT | ❌ | ❌ | ✅ |

### Password Policy

- Minimum **8 characters**
- At least **1 uppercase** letter (A–Z)
- At least **1 lowercase** letter (a–z)
- At least **1 digit** (0–9)
- At least **1 special character** (`!@#$%^&*`)
- Encrypted with **BCrypt** (strength = 12)

### Brute Force Protection

```
Failed attempt threshold : 5 times
Lock duration            : 30 minutes (or until admin unlocks)
Tracking scope           : per username + IP
Storage                  : Redis (fast TTL-based counter)
```

---

## Caching Strategy

### Two-level Cache

```
Request
  │
  ▼
[Caffeine — L1 In-Memory Cache]  →  HIT: return immediately
  │ MISS
  ▼
[Redis — L2 Distributed Cache]   →  HIT: populate L1 + return
  │ MISS
  ▼
[PostgreSQL — Source of Truth]   →  populate L2 + L1 + return
```

### Cache Usage by Feature

| Cache | Key Pattern | TTL | Purpose |
|-------|-------------|-----|---------|
| Redis | `user:{id}` | 10 min | User profile data |
| Redis | `login_attempt:{username}:{ip}` | 30 min | Failed login counter |
| Redis | `refresh_token:{token}` | 7 days | Token validity check |
| Redis | `otp:{userId}:{type}` | 5 min | OTP code |
| Caffeine | `roles` | 5 min | Role list (rarely changes) |

---

## Logging & Monitoring

### Structured Logging (Logback + JSON)

```json
{
  "timestamp": "2024-11-15T10:30:00.123Z",
  "level": "INFO",
  "logger": "c.r2s.uam.auth.service.AuthService",
  "traceId": "abc123",
  "userId": "uuid-here",
  "action": "LOGIN_SUCCESS",
  "ip": "192.168.1.1",
  "message": "User logged in successfully"
}
```

### Actuator Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Health check |
| `GET /actuator/metrics` | JVM & app metrics |
| `GET /actuator/readiness` | Kubernetes readiness probe |
| `GET /actuator/liveness` | Kubernetes liveness probe |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |

---

## Environment Profiles

### `application.yaml` (base)

```yaml
spring:
  application:
    name: uam-service
  profiles:
    active: dev
```

### `application-dev.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uam_dev
    username: postgres
    password: postgres
  jpa:
    show-sql: true
  redis:
    host: localhost
    port: 6379
app:
  jwt:
    secret: dev-secret-key
    access-token-expiry: 900       # 15 min
    refresh-token-expiry: 604800   # 7 days
logging:
  level:
    com.r2s.uam: DEBUG
```

### `application-docker.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/uam_dev
  redis:
    host: redis
```

### `application-prod.yaml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
app:
  jwt:
    secret: ${JWT_SECRET}
logging:
  level:
    com.r2s.uam: WARN
```

---

## Database Migration (Flyway)

Migration scripts located at: `src/main/resources/db/migration/`

| File | Description |
|------|-------------|
| `V1__init_user_table.sql` | Create `users`, `roles`, `authorities`, `user_roles`, `role_authorities` |
| `V2__add_login_attempt.sql` | Create `login_attempts`, `otp_codes`, `refresh_tokens` |
| `V3__add_device_table.sql` | Create `devices` for device tracking |
| `V4__add_audit_table.sql` | Create `audit_logs` |
| `V5__seed_roles.sql` | Insert default roles and authorities |

---

## Docker & CI/CD

### `docker-compose.yml`

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: uam_dev
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"

volumes:
  postgres_data:
```

### `Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### GitHub Actions CI/CD (`.github/workflows/ci.yml`)

```yaml
name: CI Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: uam_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build & Test
        run: ./mvnw clean verify
      - name: Build Docker Image
        run: docker build -t uam-service:${{ github.sha }} .
```

---

## Project Structure

```
uam/
├── auth-service/
│   ├── src/main/java/com/r2s/uam/auth/
│   │   ├── controller/          # REST Controllers
│   │   ├── service/             # Business Logic
│   │   ├── repository/          # Spring Data JPA Repos
│   │   ├── entity/              # JPA Entities
│   │   ├── dto/
│   │   │   ├── request/         # Request DTOs
│   │   │   └── response/        # Response DTOs
│   │   ├── security/            # JWT Filter, SecurityConfig
│   │   ├── exception/           # Global Exception Handler
│   │   └── config/              # App-wide Configs
│   ├── src/main/resources/
│   │   ├── db/migration/        # Flyway SQL scripts
│   │   ├── application.yaml
│   │   ├── application-dev.yaml
│   │   ├── application-docker.yaml
│   │   └── application-prod.yaml
│   └── src/test/java/
│       ├── unit/                # JUnit 5 + Mockito
│       ├── integration/         # @SpringBootTest + Testcontainers
│       └── e2e/                 # Full API flow tests
│
├── user-service/
│   └── src/main/java/com/r2s/uam/user/
│       └── ...
│
├── notification-service/
│   └── ...
│
├── audit-service/
│   └── ...
│
├── docker-compose.yml
├── Dockerfile
├── prometheus.yml
└── README.md
```

---

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- IntelliJ IDEA (recommended)
- PostgreSQL 15+ (or via Docker)

### Run with Docker Compose (recommended)

```bash
# 1. Clone the repo
git clone https://github.com/your-username/uam.git
cd uam

# 2. Start all services
docker-compose up -d

# 3. Access Swagger UI
open http://localhost:8080/swagger-ui.html

# 4. Access Grafana dashboard
open http://localhost:3000
```

### Run Locally in IntelliJ IDEA

```bash
# 1. Start only infrastructure
docker-compose up -d postgres redis

# 2. Open project in IntelliJ IDEA
#    File → Open → select uam/ folder

# 3. Set active profile: dev
#    Run → Edit Configurations → VM Options:
#    -Dspring.profiles.active=dev

# 4. Run the main class
#    AuthServiceApplication.java → Run
```

### IntelliJ IDEA Recommended Plugins

- **Lombok** — annotation processor support
- **Database Navigator** — PostgreSQL client built-in
- **EnvFile** — load `.env` for local secrets
- **Docker** — manage containers inside IDE

---

## Use Cases Summary

| # | Use Case | Group | Status |
|---|----------|-------|--------|
| UC-01 | User Registration | Auth | ✅ |
| UC-02 | Email/OTP Verification | Auth | ✅ |
| UC-03 | User Login (JWT) | Auth | ✅ |
| UC-04 | Refresh Token | Auth | ✅ |
| UC-05 | Logout | Auth | ✅ |
| UC-06 | Lock After N Failed Attempts | Auth | ✅ |
| UC-07 | Forgot Password | Auth | ✅ |
| UC-08 | Change Password | Auth | ✅ |
| UC-09 | Role-Based Authorization (RBAC) | Auth | ✅ |
| UC-10 | Authority-Based Authorization | Auth | ✅ |
| UC-11 | View Profile | User | ✅ |
| UC-12 | Update Profile | User | ✅ |
| UC-13 | Change Email | User | ✅ |
| UC-14 | Admin – Search Users | User | ✅ |
| UC-15 | Admin – Update User Info | User | ✅ |
| UC-16 | Admin – Enable/Disable/Lock User | User | ✅ |
| UC-17 | Admin – Reset Password | User | ✅ |
| UC-18 | Device Tracking | Security | ✅ |
| UC-19 | Session Management | Security | ✅ |
| UC-20 | Role Hierarchy | Security | ✅ |
| UC-21 | Brute Force Protection | Security | ✅ |
| UC-22 | CSRF Protection | Security | ✅ |
| UC-23 | Password Policy | Security | ✅ |
| UC-24 | Send Registration Email/OTP | Notification | ✅ |
| UC-25 | Send Forgot Password Email | Notification | ✅ |
| UC-26 | Admin Force Email Notifications | Notification | ✅ |
| UC-27 | Audit Login Events | Audit | ✅ |
| UC-28 | Audit User Actions | Audit | ✅ |
| UC-29 | Export Audit Logs (CSV) | Audit | ✅ |
| UC-30 | Monitoring with Actuator | Audit | ✅ |
| UC-31 | Prometheus Integration | Audit | ✅ |
| UC-32 | API Documentation (Swagger) | System | ✅ |
| UC-33 | Database Migration (Flyway) | System | ✅ |
| UC-34 | Environment Profiles | System | ✅ |

---

## License

This project is developed for educational purposes at [R2S Academy](https://ojt.r2.vn).  
Feel free to fork and use it as a portfolio project.

---

> Built with ❤️ for aspiring Junior Java Backend Developers.
