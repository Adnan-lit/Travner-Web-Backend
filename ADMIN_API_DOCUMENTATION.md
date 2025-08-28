# Admin API Documentation

## Overview

The Admin API provides comprehensive administrative controls for user management in the Travner-Web-Backend application. All admin endpoints require ADMIN role authorization and are secured using HTTP Basic Authentication.

## Base URL
```
http://localhost:8080/admin
```

## Authentication
All admin endpoints require:
- **Authentication**: HTTP Basic Authentication
- **Authorization**: User must have ADMIN role
- **Headers**: `Content-Type: application/json` for POST/PUT requests

## API Endpoints

### 1. Get All Users
**GET** `/admin/users`

Retrieves all users in the system (passwords are excluded from response).

**Example Request:**
```bash
curl -u admin:password http://localhost:8080/admin/users
```

**Response:** `200 OK`
```json
[
  {
    "id": "64f8a1b2c3d4e5f6g7h8i9j0",
    "userName": "john_doe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "roles": ["USER"]
  }
]
```

### 2. Get User by Username
**GET** `/admin/users/{username}`

Retrieves a specific user by username.

**Example Request:**
```bash
curl -u admin:password http://localhost:8080/admin/users/john_doe
```

**Response:** `200 OK` / `404 Not Found`

### 3. Delete User
**DELETE** `/admin/users/{username}`

Deletes a user by username. Admins cannot delete their own account.

**Example Request:**
```bash
curl -X DELETE -u admin:password http://localhost:8080/admin/users/john_doe
```

**Response:** `200 OK`
```json
{
  "message": "User deleted successfully"
}
```

**Error Response:** `403 Forbidden`
```json
{
  "error": "Cannot delete your own account"
}
```

### 4. Update User Roles
**PUT** `/admin/users/{username}/roles`

Updates a user's roles. Valid roles: `USER`, `ADMIN`.

**Example Request:**
```bash
curl -X PUT -u admin:password \
  -H "Content-Type: application/json" \
  -d '{"roles": ["USER", "ADMIN"]}' \
  http://localhost:8080/admin/users/john_doe/roles
```

**Response:** `200 OK`
```json
{
  "message": "User roles updated successfully"
}
```

### 5. Reset User Password
**PUT** `/admin/users/{username}/password`

Resets a user's password. Password must be at least 6 characters.

**Example Request:**
```bash
curl -X PUT -u admin:password \
  -H "Content-Type: application/json" \
  -d '{"password": "newpassword123"}' \
  http://localhost:8080/admin/users/john_doe/password
```

**Response:** `200 OK`
```json
{
  "message": "Password reset successfully"
}
```

### 6. Promote User to Admin
**POST** `/admin/users/{username}/promote`

Promotes a user to admin by adding the ADMIN role to their existing roles.

**Example Request:**
```bash
curl -X POST -u admin:password http://localhost:8080/admin/users/john_doe/promote
```

**Response:** `200 OK`
```json
{
  "message": "User promoted to admin successfully"
}
```

### 7. Get Users by Role
**GET** `/admin/users/role/{role}`

Retrieves all users with a specific role.

**Example Request:**
```bash
curl -u admin:password http://localhost:8080/admin/users/role/ADMIN
```

**Response:** `200 OK`
```json
[
  {
    "userName": "admin_user",
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com",
    "roles": ["USER", "ADMIN"]
  }
]
```

### 8. Get System Statistics
**GET** `/admin/stats`

Retrieves system statistics including user counts by role.

**Example Request:**
```bash
curl -u admin:password http://localhost:8080/admin/stats
```

**Response:** `200 OK`
```json
{
  "totalUsers": 150,
  "adminUsers": 5,
  "regularUsers": 145,
  "timestamp": 1693123456789
}
```

### 9. Create Admin User
**POST** `/admin/users`

Creates a new user with admin privileges.

**Example Request:**
```bash
curl -X POST -u admin:password \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "new_admin",
    "password": "securepass123",
    "firstName": "New",
    "lastName": "Admin",
    "email": "newadmin@example.com"
  }' \
  http://localhost:8080/admin/users
```

**Response:** `201 Created`
```json
{
  "message": "Admin user created successfully"
}
```

## Error Responses

### Common HTTP Status Codes
- **200 OK**: Request successful
- **201 Created**: Resource created successfully
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Access denied (insufficient privileges)
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists
- **500 Internal Server Error**: Server error

### Error Response Format
```json
{
  "error": "Error message description"
}
```

## Security Features

1. **Role-Based Access Control**: Only users with ADMIN role can access admin endpoints
2. **Self-Protection**: Admins cannot delete their own accounts
3. **Password Security**: Passwords are automatically hashed using BCrypt
4. **Data Protection**: Passwords are never returned in API responses
5. **Input Validation**: All inputs are validated for security and data integrity

## Usage Examples with Postman

1. **Set Authorization**: 
   - Type: Basic Auth
   - Username: your-admin-username
   - Password: your-admin-password

2. **Set Headers**:
   - Content-Type: application/json (for POST/PUT requests)

3. **Test Endpoints**: Start with GET `/admin/stats` to verify admin access

## Creating Your First Admin User

Since admin endpoints require admin privileges, you'll need to manually create your first admin user:

1. **Create a regular user** via the public endpoint:
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"userName": "admin", "password": "adminpass123"}' \
  http://localhost:8080/public/create-user
```

2. **Manually update the user's roles in MongoDB** to include "ADMIN":
```javascript
db.users.updateOne(
  {"userName": "admin"}, 
  {"$set": {"roles": ["USER", "ADMIN"]}}
)
```

3. **Now you can use the admin endpoints** with this user's credentials.

## Best Practices

1. **Use Strong Passwords**: Enforce password policies for admin accounts
2. **Regular Audits**: Monitor admin activities and user role changes
3. **Principle of Least Privilege**: Only grant admin access when necessary
4. **Secure Communication**: Always use HTTPS in production
5. **Rate Limiting**: Implement rate limiting for admin endpoints in production
6. **Logging**: Log all admin actions for security auditing

## Next Steps

Consider implementing:
- Activity logging for admin actions
- Rate limiting and request throttling
- Email notifications for critical admin actions
- Bulk user operations
- User search and filtering capabilities
- Role hierarchy and permissions system