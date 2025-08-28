# MongoDB Atlas Setup & Admin API Access Guide

## 🚀 Quick Start

### 1. Configure MongoDB Atlas

1. **Create MongoDB Atlas Account**
   - Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
   - Create a free M0 cluster

2. **Setup Database Access**
   ```
   Username: travner_user
   Password: [Generate secure password]
   Role: Read and write to any database
   ```

3. **Configure Network Access**
   - Add IP: `0.0.0.0/0` (or your specific IP)

4. **Get Connection String**
   - Format: `mongodb+srv://travner_user:PASSWORD@cluster.mongodb.net/TravnerDB?retryWrites=true&w=majority`

### 2. Update Environment Configuration

Edit the `.env` file in your project root:

```env
MONGODB_URI=mongodb+srv://travner_user:YOUR_PASSWORD@your-cluster.mongodb.net/TravnerDB?retryWrites=true&w=majority
MONGODB_DATABASE=TravnerDB
PORT=8080
```

### 3. Start the Application

```bash
./mvnw spring-boot:run
```

**Expected Output:**
```
Tomcat started on port(s): 8080 (http)
Started TravnerApplication in X.XX seconds
```

### 4. Create Admin User

**Option A: Use the provided script**
```bash
# On Windows
create-admin.bat

# On Linux/Mac
./create-admin.sh
```

**Option B: Manual API calls**
```bash
# Create initial user
curl -X POST http://localhost:8080/public/create-user \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "admin", 
    "password": "admin123456",
    "firstName": "System",
    "lastName": "Administrator",
    "email": "admin@travner.com"
  }'
```

### 5. Promote User to Admin

Since no admin exists initially, you need to manually add the ADMIN role:

**Method 1: MongoDB Atlas Dashboard**
1. Go to Collections → TravnerDB → userEntry
2. Find your admin user
3. Edit the document
4. Change `"roles": ["USER"]` to `"roles": ["USER", "ADMIN"]`

**Method 2: MongoDB Compass**
1. Connect to your cluster
2. Navigate to TravnerDB.userEntry
3. Edit the admin user document
4. Update roles array to include "ADMIN"

### 6. Test Admin Access

```bash
# Test admin endpoint
curl -u admin:admin123456 http://localhost:8080/admin/users

# Get system stats
curl -u admin:admin123456 http://localhost:8080/admin/stats
```

## 🔧 Admin API Endpoints

### User Management
- `GET /admin/users` - Get all users
- `GET /admin/users/{username}` - Get user by username
- `DELETE /admin/users/{username}` - Delete user
- `POST /admin/users` - Create admin user
- `PUT /admin/users/{username}/roles` - Update user roles
- `PUT /admin/users/{username}/password` - Reset password
- `POST /admin/users/{username}/promote` - Promote to admin

### System Management
- `GET /admin/stats` - Get system statistics
- `GET /admin/users/role/{role}` - Get users by role

## 🔐 Security Requirements

According to project specifications:
- All admin endpoints require ADMIN role authorization
- Integration with Spring Security configuration
- Authentication via HTTP Basic Auth

## 🐛 Troubleshooting

### Application Won't Start
- **Issue**: MongoDB connection timeout
- **Solution**: Verify MongoDB Atlas URI in `.env` file
- **Check**: Network access settings in Atlas

### Admin APIs Return 401
- **Issue**: User doesn't have ADMIN role
- **Solution**: Add "ADMIN" to user's roles array in MongoDB

### Admin APIs Return 403
- **Issue**: User authenticated but lacks ADMIN role
- **Solution**: Verify roles array contains "ADMIN"

### Connection Refused
- **Issue**: Application not running
- **Solution**: Check application startup logs for errors

## 📞 Support

If you encounter issues:
1. Check application logs for MongoDB connection status
2. Verify MongoDB Atlas cluster is running
3. Confirm network access settings
4. Test connection string with MongoDB Compass

## 🔒 Security Notes

- Change default admin password immediately after setup
- Use strong passwords for production
- Consider IP whitelisting instead of 0.0.0.0/0
- Store sensitive credentials in environment variables only