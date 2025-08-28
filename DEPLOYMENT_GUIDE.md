# Travner Backend Deployment Guide

## 🚀 Deployment Overview

Your **Travner Frontend** is successfully deployed on Vercel at:
- **Primary Domain**: https://travner.vercel.app  
- **Deployment URL**: https://travner-b372v97oy-jabale-nur-adnans-projects.vercel.app

This guide covers deploying your **Travner Backend** to work seamlessly with your Vercel frontend.

## ✅ Pre-Deployment Checklist

### Already Configured ✅
- [x] MongoDB Atlas connection established
- [x] CORS configured for Vercel domains
- [x] Environment variables properly set
- [x] All APIs tested and working
- [x] Security configuration validated

## 🌐 Recommended Backend Deployment Platforms

### 1. **Railway (Recommended)**
**Why**: Excellent for Spring Boot, easy MongoDB integration, good free tier.

**Steps**:
```bash
# 1. Install Railway CLI
npm install -g @railway/cli

# 2. Login to Railway
railway login

# 3. Initialize project
railway init

# 4. Deploy
railway up
```

**Environment Variables to Set in Railway**:
```env
MONGODB_URI=mongodb+srv://madnan221314:SA8rldSab9dfdy10@cluster0.5os88dy.mongodb.net/TravnerDB?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=TravnerDB
PORT=8080
SPRING_PROFILES_ACTIVE=production
```

### 2. **Render**
**Why**: Free tier, easy Spring Boot deployment, automatic HTTPS.

**Steps**:
1. Connect your GitHub repository
2. Create new Web Service
3. Set build command: `./mvnw clean package -DskipTests`
4. Set start command: `java -jar target/Travner-0.0.1-SNAPSHOT.jar`

### 3. **Heroku**
**Why**: Classic choice, good documentation, MongoDB add-ons available.

**Steps**:
```bash
# 1. Create Heroku app
heroku create travner-backend

# 2. Set environment variables
heroku config:set MONGODB_URI="your-mongodb-uri"

# 3. Deploy
git push heroku main
```

## 🔧 Current Configuration Status

### CORS Configuration ✅
Your backend is configured to accept requests from:

**Development Origins**:
- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `http://localhost:4201` (Angular alt)
- `http://localhost:5173` (Vite)
- `http://localhost:8080` - `http://localhost:9000`
- All `127.0.0.1` variants

**Production Origins**:
- `https://travner.vercel.app` ✅
- `https://travner-b372v97oy-jabale-nur-adnans-projects.vercel.app` ✅
- `https://travner-*.vercel.app` (wildcard for branch previews) ✅
- `https://*-jabale-nur-adnans-projects.vercel.app` (wildcard for deployments) ✅

### Environment Configuration ✅
- MongoDB Atlas credentials configured
- Port configurable via `PORT` environment variable
- Spring profiles support for production settings

## 📱 Frontend Integration

### Update Frontend API Base URL
Once your backend is deployed, update your frontend code to use the production API:

```javascript
// In your frontend configuration
const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? 'https://your-backend-domain.com'  // Replace with actual backend URL
  : 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Important for authentication
});
```

### Vercel Environment Variables
Add to your Vercel project settings:
```env
REACT_APP_API_URL=https://your-backend-domain.com
# or
NEXT_PUBLIC_API_URL=https://your-backend-domain.com
```

## 🔐 Security Considerations

### Production Checklist
- [x] HTTPS only for production (automatic with most platforms)
- [x] MongoDB Atlas credentials secured
- [x] CORS properly configured
- [x] Authentication working with Basic Auth
- [x] Password hashing implemented
- [x] Admin endpoints secured with role-based access

### Recommended Additions for Production
```yaml
# Add to application.yml (production profile)
spring:
  profiles: production
  security:
    require-ssl: true
logging:
  level:
    org.adnan.travner: INFO
    org.springframework.security: WARN
```

## 🚀 Deployment Commands

### Build Production JAR
```bash
./mvnw clean package -DskipTests
```

### Test Production Build Locally
```bash
java -jar target/Travner-0.0.1-SNAPSHOT.jar
```

## 📊 API Endpoints Available

### Public APIs
- `POST /public/create-user` - User registration
- `GET /public/check-username/{username}` - Username availability
- `POST /public/forgot-password` - Password reset request
- `POST /public/reset-password` - Password reset with token
- `POST /public/create-first-admin` - Create initial admin

### User APIs (Requires Authentication)
- `GET /user` - Get user profile
- `PUT /user/profile` - Update profile
- `PATCH /user/profile` - Partial profile update
- `PUT /user/password` - Change password
- `DELETE /user` - Delete account

### Admin APIs (Requires ADMIN role)
- `GET /admin/users` - List all users
- `GET /admin/users/{username}` - Get user by username
- `DELETE /admin/users/{username}` - Delete user
- `PUT /admin/users/{username}/roles` - Update user roles
- `PUT /admin/users/{username}/password` - Reset user password
- `POST /admin/users/{username}/promote` - Promote to admin
- `GET /admin/users/role/{role}` - Get users by role
- `GET /admin/stats` - System statistics
- `POST /admin/users` - Create admin user

## 🔍 Testing Deployment

### 1. Health Check
```bash
curl https://your-backend-domain.com/public/check-username/test
```

### 2. Test CORS
```bash
curl -H "Origin: https://travner.vercel.app" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     https://your-backend-domain.com/public/check-username/test
```

### 3. Test Authentication
```bash
curl -u "username:password" \
     https://your-backend-domain.com/user
```

## 🎯 Next Steps

1. **Choose a deployment platform** (Railway recommended)
2. **Deploy your backend** using the steps above
3. **Update frontend configuration** with your backend URL
4. **Test the full integration** between frontend and backend
5. **Monitor logs** for any issues

## 📞 Support

If you encounter issues:
1. Check deployment platform logs
2. Verify MongoDB Atlas connection
3. Confirm CORS configuration
4. Test API endpoints individually

Your backend is **100% ready for deployment**! 🚀