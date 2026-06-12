# 🧪 Complete API Testing Guide with Swagger UI

## 🚀 Step 1: Start the Application

### Option A: Using Maven
```bash
./mvnw spring-boot:run -pl auth-service
```

### Option B: Using IntelliJ
1. Open `AuthServiceApplication.java`
2. Right-click → Run
3. Wait for "Started AuthServiceApplication" message

---

## 📖 Step 2: Open Swagger UI

**URL:** http://localhost:8080/api/v1/swagger-ui.html

Alternative URLs:
- http://localhost:8080/api/v1/swagger-ui/index.html
- http://localhost:8080/api/v1/api-docs (OpenAPI JSON)

⚠️ **Note:** The application has context path `/api/v1` configured, so all URLs start with this prefix.

---

## 🎯 Step 3: Complete Test Flow

### Test Case 1: User Registration ✅

1. **Find endpoint:** `POST /auth/register`
2. **Click "Try it out"**
3. **Enter request body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123!",
  "fullName": "John Doe",
  "phone": "+1234567890"
}
```
4. **Click "Execute"**
5. **Expected Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Created",
  "data": {
    "id": "uuid-here",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "status": "PENDING",
    "roles": ["ROLE_USER"]
  }
}
```

6. **⚠️ IMPORTANT: Check application console for OTP code**
```
INFO ... : Generated OTP for user: john@example.com, type: VERIFY_EMAIL
```
Copy the 6-digit OTP code from logs.

---

### Test Case 2: Verify Email with OTP ✅

1. **Find endpoint:** `POST /auth/verify-otp`
2. **Click "Try it out"**
3. **Enter request body (use OTP from console):**
```json
{
  "email": "john@example.com",
  "code": "123456"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email verified successfully",
  "data": {
    "id": "uuid-here",
    "username": "johndoe",
    "status": "ACTIVE",
    "roles": ["ROLE_USER"]
  }
}
```

---

### Test Case 3: User Login ✅

1. **Find endpoint:** `POST /auth/login`
2. **Click "Try it out"**
3. **Enter request body:**
```json
{
  "usernameOrEmail": "johndoe",
  "password": "Password123!"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
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
      "id": "uuid-here",
      "username": "johndoe",
      "email": "john@example.com",
      "status": "ACTIVE"
    }
  }
}
```

6. **⚠️ COPY the accessToken** - you'll need it for protected endpoints!

---

### Test Case 4: Authorize in Swagger (for protected endpoints)

1. **Click the "Authorize" button** (🔓 icon at top right)
2. **Enter:** `Bearer YOUR_ACCESS_TOKEN_HERE`
   - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. **Click "Authorize"**
4. **Click "Close"**
5. **Now you can access protected endpoints!** 🔒

---

### Test Case 5: Change Password (Protected) ✅

1. **Make sure you're authorized** (Step 4)
2. **Find endpoint:** `POST /auth/change-password`
3. **Click "Try it out"**
4. **Enter request body:**
```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!"
}
```
5. **Click "Execute"**
6. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Password changed successfully",
  "data": null
}
```

---

### Test Case 6: Logout ✅

1. **Find endpoint:** `POST /auth/logout`
2. **Click "Try it out"**
3. **Enter request body (optional):**
```json
{
  "refreshToken": "your-refresh-token-from-login"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Logout successful",
  "data": null
}
```

---

### Test Case 7: Forgot Password ✅

1. **Find endpoint:** `POST /auth/forgot-password`
2. **Click "Try it out"**
3. **Enter request body:**
```json
{
  "email": "john@example.com"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Password reset OTP sent to your email",
  "data": null
}
```
6. **Check console for OTP code**

---

### Test Case 8: Reset Password ✅

1. **Find endpoint:** `POST /auth/reset-password`
2. **Click "Try it out"**
3. **Enter request body (use OTP from console):**
```json
{
  "email": "john@example.com",
  "code": "654321",
  "newPassword": "ResetPassword789!"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Password reset successful",
  "data": null
}
```

---

### Test Case 9: Refresh Access Token ✅

1. **Find endpoint:** `POST /auth/refresh-token`
2. **Click "Try it out"**
3. **Enter request body (use refreshToken from login):**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-1234567890"
}
```
4. **Click "Execute"**
5. **Expected Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new-jwt-token-here",
    "refreshToken": "new-refresh-token-here",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

---

## 📊 All Available Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/auth/register` | ❌ No | Register new user |
| POST | `/auth/verify-otp` | ❌ No | Verify email with OTP |
| POST | `/auth/login` | ❌ No | User login |
| POST | `/auth/refresh-token` | ❌ No | Refresh access token |
| POST | `/auth/forgot-password` | ❌ No | Request password reset |
| POST | `/auth/reset-password` | ❌ No | Reset password with OTP |
| POST | `/auth/logout` | ✅ Yes | Logout user |
| POST | `/auth/change-password` | ✅ Yes | Change password |

---

## 🎯 Testing Checklist

- [ ] **Test 1:** Register new user → Status PENDING
- [ ] **Test 2:** Verify OTP → Status ACTIVE
- [ ] **Test 3:** Login → Get JWT tokens
- [ ] **Test 4:** Authorize in Swagger with JWT
- [ ] **Test 5:** Change password (protected)
- [ ] **Test 6:** Logout
- [ ] **Test 7:** Forgot password → Get OTP
- [ ] **Test 8:** Reset password with OTP
- [ ] **Test 9:** Refresh access token
- [ ] **Test 10:** Try login with new password

---

## ⚠️ Common Issues & Solutions

### Issue 1: "401 Unauthorized" on protected endpoints
**Solution:** Click "Authorize" button and enter: `Bearer YOUR_TOKEN`

### Issue 2: Can't find OTP code
**Solution:** Check application console/logs:
```bash
# Search for OTP in logs
docker-compose logs auth-service | grep "OTP"
# OR in your terminal where app is running
```

### Issue 3: "Invalid or expired OTP"
**Solution:** 
- OTP expires in 5 minutes
- Generate new OTP by registering again or requesting forgot password

### Issue 4: "Email is already in use"
**Solution:** Use different email or check database:
```sql
-- Connect to database
psql -U postgres -d uam_dev

-- View all users
SELECT username, email, status FROM users;

-- Delete test user
DELETE FROM users WHERE email = 'john@example.com';
```

### Issue 5: Swagger UI not loading (404 error)
**Solution:**
- Clear browser cache
- Try incognito mode
- Check correct URL: http://localhost:8080/api/v1/swagger-ui.html (note the `/api/v1` prefix)
- Verify app is running: `curl http://localhost:8080/api/v1/actuator/health`

---

## 🔍 Verify Database

After testing, check data in database:

```sql
-- Connect to database
psql -U postgres -d uam_dev

-- View users
SELECT id, username, email, status FROM users;

-- View roles
SELECT u.username, r.name as role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON r.id = ur.role_id;

-- View OTP codes
SELECT user_id, code, type, used, expires_at 
FROM otp_codes 
ORDER BY created_at DESC 
LIMIT 10;

-- View login attempts
SELECT username_try, success, ip_address, attempted_at 
FROM login_attempts 
ORDER BY attempted_at DESC 
LIMIT 10;

-- View refresh tokens
SELECT user_id, revoked, expires_at, created_at 
FROM refresh_tokens 
ORDER BY created_at DESC 
LIMIT 10;
```

---

## 📈 Success Metrics

After completing all tests:

✅ **Functionality:**
- Registration works ✓
- Email verification works ✓
- Login returns JWT ✓
- Protected endpoints require auth ✓
- Password management works ✓
- Token refresh works ✓

✅ **Database:**
- Users table has data ✓
- Roles are assigned ✓
- OTP codes are generated ✓
- Refresh tokens are stored ✓

✅ **Security:**
- Passwords are hashed ✓
- JWT tokens are valid ✓
- Protected endpoints secured ✓

---

## 🎉 Congratulations!

You've successfully tested the complete authentication system!

**Next Steps:**
1. Test edge cases (invalid data, expired tokens)
2. Check audit logs
3. Test with different users
4. Try invalid credentials
5. Test concurrent logins

**For production:**
- Configure real SMTP for emails
- Set up Redis for caching
- Enable rate limiting
- Add monitoring dashboards

---

**Need help?** See `TROUBLESHOOTING.md` for detailed debugging guide!
