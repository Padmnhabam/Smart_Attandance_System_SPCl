# JWT Security Testing - Quick Reference

## 🧪 Test Your JWT Implementation

### Prerequisites
- Spring Boot app running on `http://localhost:8080`
- MySQL database connected
- At least one teacher account created

---

## Test 1: Login & Get JWT Token

**Using Postman or cURL:**

```bash
curl -X POST http://localhost:8080/api/teachers/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teacher@example.com",
    "password": "your_password"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZWFjaGVyQGV4YW1wbGUuY29tIiwicm9sZSI6IlRFQUNIRVIiLCJpYXQiOjE2OTMwMDAwMDAsImV4cCI6MTY5MzA4NjQwMH0.xxxxx",
  "role": "teacher",
  "userPayload": {
    "id": 1,
    "name": "John Doe",
    "email": "teacher@example.com",
    "role": "teacher"
  }
}
```

**Save the token value** - you'll need it for the next tests.

---

## Test 2: Use Token to Access Protected Teacher API

**Command (Replace TOKEN with your token from Test 1):**

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...."

curl -X GET http://localhost:8080/api/teachers/teacher@example.com \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "teacher@example.com",
  "department": "Computer Science",
  "mobilenumber": "9876543210"
}
```

**If 401 Unauthorized:** Token is invalid/expired - get a new one from Test 1

---

## Test 3: Try to Access Protected API WITHOUT Token (Should Fail)

```bash
curl -X GET http://localhost:8080/api/teachers/teacher@example.com
```

**Expected Response (401 or filtered):**
- No Authorization header
- Request should be blocked/ignored

---

## Test 4: Access Attendance API WITH Token (Should Work)

First, you need a student or session ID to mark attendance. But the endpoint should accept the request with valid token:

```bash
TOKEN="your_token_here"

curl -X POST http://localhost:8080/api/attendance/mark \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "rollNo=1&subject=Math&deviceId=device123&latitude=40.7128&longitude=-74.0060&sessionId=1"
```

**Expected Response:**
- ✅ 200 OK if session exists and teacher has token
- ❌ 401 Unauthorized if no token
- ❌ 403 Forbidden if token is from student (not TEACHER role)

---

## Test 5: Test Expired Token (Optional)

Wait 24+ hours OR modify token to expire sooner in properties, then:

```bash
curl -X GET http://localhost:8080/api/teachers/teacher@example.com \
  -H "Authorization: Bearer old_expired_token"
```

**Expected Response:**
- ❌ 401 Unauthorized (token expired)

---

## 🌐 Browser Frontend Testing

### Test 6: Test Full Login Flow in Browser

1. Visit `http://localhost:8080/public/login.html`
2. Select "Teacher" from role dropdown
3. Enter your teacher credentials
4. Click "Sign In"

**Expected:**
- ✅ Login successful message
- ✅ Redirect to teacher-dashboard.html
- ✅ Token stored in browser localStorage

### Verify Token in Browser:

1. Open DevTools (F12)
2. Go to Storage → Local Storage → localhost:8080
3. Look for `authToken` key
4. Value should be a long JWT string starting with `eyJ...`

### Test 7: Check Automatic Token Injection

1. Open DevTools → Network tab
2. In teacher dashboard, click any button that makes an API call
3. Look at any request to `/api/...`
4. Check the "Headers" tab
5. Look for: `Authorization: Bearer eyJ...`

**Expected:**
- ✅ Every API request includes Authorization header
- ✅ No manual header addition needed (fetch interceptor handles it)

---

## 🔴 Troubleshooting

### Issue: "Invalid email or password"
- **Cause**: Email doesn't exist in database OR password is wrong
- **Fix**: Create new teacher account or use correct credentials

### Issue: 401 Unauthorized
- **Cause 1**: No Authorization header sent
  - Fix: Add header when testing with cURL
- **Cause 2**: Token is expired (24 hours old)
  - Fix: Login again to get new token
- **Cause 3**: Token is tampered/invalid
  - Fix: Don't modify the token string

### Issue: 403 Forbidden
- **Cause**: User token doesn't have required role
- **Fix**: Make sure you're testing with TEACHER or ADMIN account

### Issue: Not seeing Authorization header in requests
- **Cause**: Fetch interceptor might be disabled or not working
- **Fix**: 
  1. Check if you're in correct browser tab (where dashboard loaded)
  2. Reload the page
  3. Check browser console for errors

### Issue: Token not stored in localStorage
- **Cause**: Login failed OR response didn't include token
- **Fix**: Check browser console for errors, then login again

---

## 📋 Quick Test Checklist

- [ ] **Test 1 PASS**: Login successful, received token
- [ ] **Test 2 PASS**: Accessed protected API with token
- [ ] **Test 3 PASS**: Request blocked without token
- [ ] **Test 4 PASS**: Attendance API requires token
- [ ] **Test 6 PASS**: Frontend login successful
- [ ] **Test 7 PASS**: Authorization header auto-added to requests

---

## 🎯 When Everything Works:

You'll see:
- ✅ Login page accepts credentials
- ✅ Token stored in browser localStorage
- ✅ Dashboards load without "Unauthorized" errors
- ✅ API calls automatically include JWT token
- ✅ Attendance marking requires TEACHER role
- ✅ Invalid/expired tokens are rejected

**Congratulations! Your JWT security is working perfectly! 🔐**

---

## 📝 For Your Next Steps:

1. **Test the system thoroughly** using tests above
2. **Demo to your instructor/examiner** how security works
3. **Explain the flow** (login → token → protected API)
4. **Show frontend code** that adds Authorization header
5. **Show SecurityConfig** that restricts attendance API

This demonstrates enterprise-level API security! 🚀
