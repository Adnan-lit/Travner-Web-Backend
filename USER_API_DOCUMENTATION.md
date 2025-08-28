# User API Documentation

## Overview

The User API provides comprehensive user management functionality for the Travner-Web-Backend application. It includes authenticated user endpoints for profile management and public endpoints for registration and password recovery.

## Base URLs
```
http://localhost:8080/user    (Authenticated endpoints)
http://localhost:8080/public  (Public endpoints)
```

## Authentication
User endpoints require:
- **Authentication**: HTTP Basic Authentication
- **Authorization**: User must be authenticated (USER or ADMIN role)
- **Headers**: `Content-Type: application/json` for POST/PUT/PATCH requests

Public endpoints require no authentication.

## API Endpoints

## Authenticated User Endpoints

### 1. Get Current User Profile
**GET** `/user`

Retrieves the current authenticated user's profile (password excluded).

**Example Request:**
```bash
curl -u username:password http://localhost:8080/user
```

**Response:** `200 OK`
```json
{
  "id": "64f8a1b2c3d4e5f6g7h8i9j0",
  "userName": "john_doe",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

**Error Response:** `401 Unauthorized`

### 2. Update User Profile (Full Update)
**PUT** `/user/profile`

Updates the user's profile information (full replacement).

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com"
}
```

**Example Request:**
```bash
curl -X PUT -u username:password \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe", 
    "email": "john.doe@example.com"
  }' \
  http://localhost:8080/user/profile
```

**Response:** `200 OK`
```json
{
  "message": "Profile updated successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid email format
- `401 Unauthorized`: Authentication required
- `500 Internal Server Error`: Update failed

### 3. Update User Profile (Partial Update)
**PATCH** `/user/profile`

Partially updates the user's profile information (only provided fields).

**Request Body:**
```json
{
  "firstName": "John"
}
```

**Example Request:**
```bash
curl -X PATCH -u username:password \
  -H "Content-Type: application/json" \
  -d '{"firstName": "John"}' \
  http://localhost:8080/user/profile
```

**Response:** `200 OK`
```json
{
  "message": "Profile updated successfully"
}
```

**Notes:**
- Only provided fields will be updated
- Sensitive fields (password, userName, roles, id) are automatically excluded
- Empty request body returns `400 Bad Request`

### 4. Change Password
**PUT** `/user/password`

Changes the user's password. Requires current password for verification.

**Request Body:**
```json
{
  "currentPassword": "current123",
  "newPassword": "newpassword123"
}
```

**Example Request:**
```bash
curl -X PUT -u username:password \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "current123",
    "newPassword": "newpassword123"
  }' \
  http://localhost:8080/user/password
```

**Response:** `200 OK`
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Current password incorrect, new password too short (<6 chars), or missing fields
- `401 Unauthorized`: Authentication required
- `500 Internal Server Error`: Password update failed

### 5. Delete User Account
**DELETE** `/user`

Deletes the current user's account permanently.

**Example Request:**
```bash
curl -X DELETE -u username:password http://localhost:8080/user
```

**Response:** `200 OK`

**Error Responses:**
- `401 Unauthorized`: Authentication required
- `500 Internal Server Error`: Deletion failed

## Public Endpoints

### 6. Create New User Account
**POST** `/public/create-user`

Creates a new user account (registration).

**Request Body:**
```json
{
  "userName": "john_doe",
  "password": "securepass123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

**Example Request:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "userName": "john_doe",
    "password": "securepass123",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  }' \
  http://localhost:8080/public/create-user
```

**Response:** `201 Created`

**Error Responses:**
- `400 Bad Request`: Invalid or missing user data
- `409 Conflict`: Username already exists
- `500 Internal Server Error`: Account creation failed

### 7. Check Username Availability
**GET** `/public/check-username/{username}`

Checks if a username is available for registration.

**Example Request:**
```bash
curl http://localhost:8080/public/check-username/john_doe
```

**Response:** `200 OK`
```json
{
  "message": "Username is available",
  "available": true
}
```

**Response (Username Taken):** `200 OK`
```json
{
  "message": "Username is already taken",
  "available": false
}
```

**Error Responses:**
- `400 Bad Request`: Invalid username format (must be 3-50 alphanumeric characters or underscore)
- `500 Internal Server Error`: Check failed

**Username Validation Rules:**
- 3-50 characters in length
- Alphanumeric characters and underscore only
- Regex pattern: `^[a-zA-Z0-9_]{3,50}$`

### 8. Request Password Reset
**POST** `/public/forgot-password`

Initiates a password reset process. Returns success regardless of username existence for security.

**Request Body:**
```json
{
  "username": "john_doe"
}
```

**Example Request:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"username": "john_doe"}' \
  http://localhost:8080/public/forgot-password
```

**Response:** `200 OK`
```json
{
  "message": "If the username exists, a password reset token has been generated. In production, this would be sent via email.",
  "resetToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Notes:**
- The `resetToken` field is only included for testing purposes
- In production, the token should be sent via email instead of returned in response
- Tokens expire after 15 minutes
- Always returns success to prevent username enumeration

### 9. Reset Password with Token
**POST** `/public/reset-password`

Resets a user's password using a valid reset token.

**Request Body:**
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "newPassword": "newpassword123"
}
```

**Example Request:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "newPassword": "newpassword123"
  }' \
  http://localhost:8080/public/reset-password
```

**Response:** `200 OK`
```json
{
  "message": "Password reset successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid/expired token, missing fields, or password too short (<6 chars)
- `500 Internal Server Error`: Password reset failed

## Data Validation Rules

### Password Requirements
- Minimum length: 6 characters
- No maximum length restriction
- Can contain any characters

### Email Validation
- Must follow standard email format
- Regex pattern: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$`

### Username Validation
- Length: 3-50 characters
- Characters: Alphanumeric and underscore only
- Case sensitive
- Must be unique across all users

## Error Response Format

All error responses follow this format:
```json
{
  "error": "Error message description"
}
```

## Common HTTP Status Codes

- **200 OK**: Request successful
- **201 Created**: Resource created successfully
- **400 Bad Request**: Invalid request data or validation failed
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Access denied
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists (e.g., username taken)
- **500 Internal Server Error**: Server error

## Security Features

1. **Password Security**: 
   - Passwords are hashed using BCrypt
   - Passwords never returned in API responses
   - Current password verification required for changes

2. **Input Validation**: 
   - All inputs validated for format and security
   - Sensitive fields automatically excluded from updates

3. **Authentication**: 
   - HTTP Basic Authentication for user endpoints
   - Session-based authentication support

4. **Rate Limiting**: 
   - Consider implementing for password reset endpoints
   - Recommended for production deployment

## Usage Examples with Postman

### Setting up Authentication
1. **Set Authorization**: 
   - Type: Basic Auth
   - Username: your-username
   - Password: your-password

2. **Set Headers**:
   - Content-Type: application/json (for POST/PUT/PATCH requests)

### Testing Flow
1. **Create Account**: `POST /public/create-user`
2. **Login**: `GET /user` (with Basic Auth)
3. **Update Profile**: `PATCH /user/profile`
4. **Change Password**: `PUT /user/password`

## Complete User API Workflow

### New User Registration
```bash
# 1. Check username availability
curl http://localhost:8080/public/check-username/newuser

# 2. Create account if available
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "userName": "newuser",
    "password": "securepass123",
    "firstName": "New",
    "lastName": "User",
    "email": "newuser@example.com"
  }' \
  http://localhost:8080/public/create-user
```

### User Profile Management
```bash
# 1. Get current profile
curl -u newuser:securepass123 http://localhost:8080/user

# 2. Update profile partially
curl -X PATCH -u newuser:securepass123 \
  -H "Content-Type: application/json" \
  -d '{"email": "updated@example.com"}' \
  http://localhost:8080/user/profile

# 3. Change password
curl -X PUT -u newuser:securepass123 \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "securepass123",
    "newPassword": "newsecurepass456"
  }' \
  http://localhost:8080/user/password
```

### Password Recovery
```bash
# 1. Request password reset
curl -X POST -H "Content-Type: application/json" \
  -d '{"username": "newuser"}' \
  http://localhost:8080/public/forgot-password

# 2. Reset password with token
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "token": "received-reset-token",
    "newPassword": "resetpassword789"
  }' \
  http://localhost:8080/public/reset-password
```

## Integration with Admin API

Users with ADMIN role can access both User API endpoints and Admin API endpoints. The User API focuses on self-service operations, while the Admin API provides administrative controls over all users.

## Best Practices

1. **Password Security**: 
   - Use strong passwords (consider enforcing complexity rules)
   - Change passwords regularly
   - Never share credentials

2. **Profile Management**: 
   - Keep profile information up to date
   - Use valid email addresses for password recovery
   - Verify email changes if implemented

3. **API Usage**: 
   - Use HTTPS in production
   - Implement proper error handling in client applications
   - Cache user profile data appropriately

4. **Security Considerations**: 
   - Implement session management
   - Add rate limiting for sensitive endpoints
   - Log security-relevant events
   - Consider implementing CAPTCHA for public endpoints

## Production Considerations

### Email Integration
- Integrate with email service for password reset tokens
- Remove reset token from API responses
- Implement email verification for new accounts

### Enhanced Security
- Implement account lockout after failed attempts
- Add two-factor authentication support
- Implement session management
- Add audit logging

### Performance Optimization
- Implement caching for user profiles
- Add pagination for large datasets
- Optimize database queries
- Implement API rate limiting

### Monitoring and Analytics
- Track user registration metrics
- Monitor password reset usage
- Log security events
- Implement health checks