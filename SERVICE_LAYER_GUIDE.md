# 📋 Service Layer Overview - User Access Management System

## 🎯 Service Architecture

The service layer contains the business logic for authentication and user management. All services are located in:
```
auth-service/src/main/java/com/r2s/uam/auth/service/
```

---

## 📁 Service Files

### 1. **AuthService.java** - Main Authentication Service
**Location:** `com.r2s.uam.auth.service.AuthService`

**Key Methods:**
- `register(RegisterRequest)` - Register new user with PENDING status
- `verifyOtp(VerifyOtpRequest)` - Verify email and activate account
- `login(LoginRequest)` - Authenticate and return JWT tokens
- `refreshToken(RefreshTokenRequest)` - Refresh access token
- `logout(String)` - Logout and revoke refresh token
- `forgotPassword(ForgotPasswordRequest)` - Send password reset OTP
- `resetPassword(ResetPasswordRequest)` - Reset password with OTP
- `changePassword(ChangePasswordRequest, String)` - Change password for authenticated user

**Dependencies:**
- UserRepository
- RoleRepository
- PasswordEncoder (BCrypt)
- JwtTokenProvider
- RefreshTokenService
- OtpService
- EmailService
- UserMapper
- AuthenticationManager

**Key Features:**
- ✅ Transaction management with `@Transactional`
- ✅ Comprehensive validation and error handling
- ✅ Security checks (PENDING, LOCKED, DISABLED status)
- ✅ Automatic role assignment
- ✅ Password encryption with BCrypt
- ✅ JWT token generation
- ✅ Detailed logging

---

### 2. **OtpService.java** - OTP Generation & Validation
**Location:** `com.r2s.uam.auth.service.OtpService`

**Key Methods:**
- `generateOtp(User, OtpType)` - Generate 6-digit OTP code
- `validateOtp(User, String, OtpType)` - Validate OTP code
- `validateAndVerifyOtp(User, String, OtpType)` - Validate and throw exception if invalid
- `cleanupExpiredOtps()` - Delete expired/used OTP codes

**Configuration:**
```yaml
app:
  otp:
    length: 6
    expiry-minutes: 5
```

**OTP Types:**
- `VERIFY_EMAIL` - Email verification
- `RESET_PASSWORD` - Password reset
- `CHANGE_EMAIL` - Email change (future)

**Key Features:**
- ✅ Secure random OTP generation using `SecureRandom`
- ✅ Automatic invalidation of old OTPs
- ✅ Expiration handling (5 minutes)
- ✅ One-time use enforcement
- ✅ Type-specific OTP validation

---

### 3. **EmailService.java** - Email Notifications
**Location:** `com.r2s.uam.auth.service.EmailService`

**Key Methods:**
- `sendOtpEmail(String, String, String)` - Send OTP code via email
- `sendWelcomeEmail(String, String)` - Send welcome email after verification
- `sendPasswordResetEmail(String, String)` - Send password reset OTP

**Email Templates:**
1. **OTP Email** - Generic OTP with purpose
2. **Welcome Email** - Account activation confirmation
3. **Password Reset Email** - Password reset instructions with OTP

**Key Features:**
- ✅ Async email sending with `@Async`
- ✅ Professional email templates
- ✅ Error handling with logging
- ✅ No blocking on email failures

**Development Mode:**
- OTP codes are logged to console
- Email sending may fail (fake SMTP)
- Check logs for OTP: `grep "Generated OTP"`

---

### 4. **RefreshTokenService.java** - Refresh Token Management
**Location:** `com.r2s.uam.auth.service.RefreshTokenService`

**Key Methods:**
- `createRefreshToken(User, UUID)` - Create new refresh token
- `validateRefreshToken(String)` - Validate and return refresh token
- `revokeToken(String)` - Revoke single token
- `revokeAllUserTokens(User)` - Revoke all tokens for user
- `cleanupExpiredTokens()` - Delete expired tokens

**Token Configuration:**
```yaml
app:
  jwt:
    refresh-token-expiry: 604800  # 7 days
```

**Key Features:**
- ✅ UUID-based token generation
- ✅ Device tracking per token
- ✅ Expiration validation
- ✅ Revocation support
- ✅ Automatic cleanup

**Security:**
- Tokens are revoked on logout
- All tokens revoked on password change
- Expired tokens automatically cleaned up

---

## 🔄 Service Flow Diagrams

### Registration Flow
```
User → AuthService.register()
  ↓
1. Validate username/email uniqueness
2. Encode password with BCrypt
3. Assign ROLE_USER
4. Save user with PENDING status
  ↓
OtpService.generateOtp(VERIFY_EMAIL)
  ↓
EmailService.sendOtpEmail()
  ↓
Return UserResponse
```

### Login Flow
```
User → AuthService.login()
  ↓
1. Find user by username/email
2. Check user status (PENDING/LOCKED/DISABLED)
3. Authenticate with AuthenticationManager
  ↓
JwtTokenProvider.generateAccessToken()
RefreshTokenService.createRefreshToken()
  ↓
Return AuthResponse with tokens
```

### OTP Verification Flow
```
User → AuthService.verifyOtp()
  ↓
1. Find user by email
2. Check if already ACTIVE
  ↓
OtpService.validateAndVerifyOtp()
  ↓
3. Update user status to ACTIVE
4. Mark OTP as used
  ↓
EmailService.sendWelcomeEmail()
  ↓
Return UserResponse
```

### Password Reset Flow
```
User → AuthService.forgotPassword()
  ↓
OtpService.generateOtp(RESET_PASSWORD)
  ↓
EmailService.sendPasswordResetEmail()
  ↓
User → AuthService.resetPassword()
  ↓
1. Validate OTP
2. Encode new password
3. Save user
  ↓
RefreshTokenService.revokeAllUserTokens()
```

---

## 🔐 Security Features

### Password Security
- **BCrypt hashing** with strength 12
- **Password validation** (min 8 chars, uppercase, lowercase, digit, special char)
- **Password history** check (current vs new)
- **All tokens revoked** on password change

### Token Security
- **JWT** for stateless authentication
- **Refresh token rotation** on refresh
- **Token revocation** support
- **Device tracking** per token
- **Automatic expiration** (15 min access, 7 days refresh)

### OTP Security
- **Secure random** generation
- **One-time use** enforcement
- **5-minute expiration**
- **Type-specific** validation
- **Automatic cleanup** of old codes

### Account Security
- **Status checks** (PENDING, ACTIVE, LOCKED, DISABLED)
- **Email verification** required
- **Role-based access control**
- **Audit logging** (ready)

---

## 📊 Database Operations

### Transactions
All write operations use `@Transactional`:
- Ensures data consistency
- Automatic rollback on errors
- Prevents partial updates

### Read Operations
Some use `@Transactional(readOnly = true)`:
- Performance optimization
- No dirty checking overhead

---

## 🎓 Best Practices Used

1. **Constructor Injection** with Lombok `@RequiredArgsConstructor`
2. **Logging** with SLF4J (`@Slf4j`)
3. **Service Layer Pattern** - business logic isolated
4. **DTO Pattern** - entity-DTO separation
5. **Exception Handling** - custom exceptions
6. **Async Processing** - non-blocking email
7. **Configuration Properties** - externalized config
8. **Transaction Management** - ACID compliance

---

## 🧪 Testing the Services

### Via Swagger UI
```
1. Open: http://localhost:8080/swagger-ui.html
2. Test each endpoint in order:
   - Register → Verify → Login → Protected endpoints
```

### Via cURL
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test123!","fullName":"Test User"}'

# Check logs for OTP
# Verify
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"123456"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"test","password":"Test123!"}'
```

---

## 📝 Service Dependencies

```
AuthService
├── UserRepository (DB)
├── RoleRepository (DB)
├── PasswordEncoder (BCrypt)
├── JwtTokenProvider (JWT)
├── RefreshTokenService
│   ├── RefreshTokenRepository (DB)
│   └── JwtTokenProvider (JWT)
├── OtpService
│   └── OtpCodeRepository (DB)
├── EmailService
│   └── JavaMailSender (SMTP)
└── UserMapper (DTO)
```

---

## 🔍 How to View Service Code

### In IntelliJ IDEA
1. Navigate to: `auth-service/src/main/java/com/r2s/uam/auth/service/`
2. Open any service file
3. Use `Ctrl+Click` to navigate to dependencies

### Via Command Line
```bash
# View AuthService
cat auth-service/src/main/java/com/r2s/uam/auth/service/AuthService.java

# View all services
ls auth-service/src/main/java/com/r2s/uam/auth/service/
```

### Service Files List
- `AuthService.java` (225 lines)
- `OtpService.java` (94 lines)
- `EmailService.java` (107 lines)
- `RefreshTokenService.java` (77 lines)

**Total:** 503 lines of business logic

---

## 🚀 Next Steps

1. ✅ **Services are complete and working**
2. ⏳ Add Redis caching layer
3. ⏳ Add brute force protection
4. ⏳ Add user profile management
5. ⏳ Add admin operations
6. ⏳ Add audit logging service

---

**All service code is ready and can be viewed in your IDE!** 🎉
