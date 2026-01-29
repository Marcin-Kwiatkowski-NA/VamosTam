# Security - Authentication & Authorization

> **Status: NOT PRODUCTION READY**
>
> This authentication implementation is functional but requires additional hardening before production use.

## Overview

The Vamos backend uses JWT-based stateless authentication for the REST API.

## Authentication Flow

```
1. Client → POST /auth/login (or /register, /google)
2. Server validates credentials
3. Server returns { accessToken, refreshToken, user }
4. Client stores tokens securely
5. Client → Protected endpoints with "Authorization: Bearer {accessToken}"
6. Server validates token from JWT claims (no DB hit)
7. When accessToken expires → POST /auth/refresh with refreshToken
```

### Flutter Client Implementation

The Flutter app implements automatic token refresh with:
- **Reactive refresh**: Only refreshes on 401 responses (not proactively)
- **Mutex pattern**: Prevents concurrent refresh calls when multiple requests fail
- **Max 1 retry**: Requests marked as retried won't trigger another refresh
- **Separate HTTP client**: Uses `http` package for refresh to avoid Dio interceptor recursion

See `memory/flutter-architecture.md` → "JWT Token Management" for details.

## Endpoints

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/auth/login` | POST | No | Login with username/password |
| `/auth/register` | POST | No | Register new user |
| `/auth/google` | POST | No | Login/register with Google ID token (**NOT READY**) |
| `/auth/refresh` | POST | No | Get new tokens using refresh token (see contract below) |
| `/auth/me` | GET | Yes | Get current user info |

### Google OAuth Status

> **Google OAuth is NOT READY** - Server access to Google APIs is not yet configured.
> The `/auth/google` endpoint code exists but cannot be used until Google Cloud credentials are set up.

### API Response Contract

All successful auth responses (`/auth/login`, `/auth/register`, `/auth/google`, `/auth/refresh`) return:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshExpiresIn": 604800,
  "user": {
    "id": 1,
    "email": "user@example.com",
    ...
  }
}
```

### Refresh Token Endpoint

**Request:**
```json
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJ..."
}
```

**Success (200):** Returns full `AuthResponse` with new tokens

**Failure (401):** Refresh token invalid or expired - client should log out user

## Token Configuration

| Token | Duration | Claim Contents |
|-------|----------|----------------|
| Access Token | 1 hour | userId, email, role, type=access |
| Refresh Token | 7 days | userId, type=refresh |

Configured in `application.properties`:
```properties
app.jwt.expiration-ms=3600000          # 1 hour
app.jwt.refresh-expiration-ms=604800000 # 7 days
```

## Authorization Rules

### Public Endpoints (No Token Required)
- `POST /auth/**` - All auth endpoints
- `GET /cities/**` - City data
- `GET /rides/search` - Ride search
- `/h2-console/**` - Dev console

### Protected Endpoints (Token Required)
- All other endpoints require a valid access token

## Key Components

| Component | Purpose |
|-----------|---------|
| `JwtAuthenticationFilter` | Validates JWT on every request (stateless - no DB hit) |
| `JwtTokenProvider` | Generates and validates JWT tokens |
| `AuthService` | Business logic for auth operations (transactional) |
| `AuthController` | REST endpoints (thin - delegates to service) |

## Security Configuration

Location: `config/security/WebAuthorizationConfig.java`

- **Session**: `STATELESS` - No server-side sessions
- **CSRF**: Disabled (not needed for stateless JWT)
- **Form Login**: Configured but `NOT_USED` - kept for potential future web UI

## NOT_USED Components

The following are configured but currently not used (kept for future web UI):
- `CustomAuthenticationSuccessHandler`
- `CustomAuthenticationFailureHandler`
- Form login configuration in `WebAuthorizationConfig`

## Production Readiness Checklist

Before going to production, address the following:

- [ ] **Google OAuth**: Configure Google Cloud credentials for OAuth
- [ ] **Token Revocation**: Implement token blacklist for logout/password change
- [ ] **Rate Limiting**: Add rate limiting on auth endpoints
- [ ] **Audit Logging**: Log authentication attempts
- [ ] **Account Lockout**: Lock accounts after failed login attempts
- [ ] **Password Policy**: Enforce password complexity rules
- [ ] **HTTPS Only**: Enforce HTTPS in production
- [ ] **Secure Headers**: Add security headers (HSTS, CSP, etc.)
- [ ] **Secret Rotation**: Plan for JWT secret rotation
- [ ] **Refresh Token Rotation**: Rotate refresh token on use (one-time use)
- [ ] **Device/Session Management**: Allow users to see/revoke sessions

## Password Hashing

BCrypt with strength 10 (`UserManagementConfig.java`)

## Roles

Defined in `traveler/Role.java`:
- `DRIVER` - Can create rides
- `PASSENGER` - Can book rides
- `ADMIN` - Administrative access

All roles implement `GrantedAuthority` with prefix `ROLE_` (e.g., `ROLE_DRIVER`).
