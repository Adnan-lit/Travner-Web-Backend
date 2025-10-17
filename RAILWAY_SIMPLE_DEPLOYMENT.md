# 🚀 Railway Simple Deployment (No Docker)

## ✅ **Simple Railway Deployment Setup**

### **📁 Files Created:**
- `railway.json` - Railway configuration
- `build.sh` - Linux build script
- `build.bat` - Windows build script

### **🗑️ Files Removed:**
- `Dockerfile` - Not needed
- `Dockerfile.temurin` - Not needed  
- `docker-compose.yml` - Not needed

## 🚀 **How Railway Auto-Deployment Works:**

### **1. Railway Detects Spring Boot Project**
Railway automatically detects:
- `pom.xml` → Maven project
- `mvnw` → Maven wrapper
- Java source files → Spring Boot application

### **2. Railway Builds Automatically**
```bash
# Railway runs this automatically:
./mvnw clean package -DskipTests
java -jar target/Travner-0.0.1-SNAPSHOT.jar
```

### **3. Railway Deploys**
- **Port**: Automatically assigned (usually 8080)
- **Health Check**: `/actuator/health`
- **Environment**: Production profile

## 🔧 **Railway Configuration:**

### **railway.json**
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS"
  },
  "deploy": {
    "startCommand": "java -jar target/Travner-0.0.1-SNAPSHOT.jar",
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 300,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 3
  }
}
```

## 🌍 **Environment Variables Needed:**

Set these in Railway dashboard:
```bash
# MongoDB Atlas
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/database
MONGODB_DATABASE=TravnerDB

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Optional
PORT=8080
JAVA_OPTS=-Xms512m -Xmx1024m
```

## 🚀 **Deployment Steps:**

### **1. Push to GitHub**
```bash
git add .
git commit -m "feat: add Railway deployment configuration"
git push origin main
```

### **2. Connect Railway to GitHub**
1. Go to [Railway.app](https://railway.app)
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Choose your repository
5. Railway will auto-detect Spring Boot

### **3. Set Environment Variables**
In Railway dashboard:
- Go to your project
- Click "Variables" tab
- Add the environment variables above

### **4. Deploy**
Railway will automatically:
- Build the project with Maven
- Create the JAR file
- Deploy and start the application

## 📊 **Railway Auto-Detection:**

Railway automatically detects:
- ✅ **Java 21** (from `pom.xml`)
- ✅ **Spring Boot** (from dependencies)
- ✅ **Maven** (from `pom.xml` and `mvnw`)
- ✅ **Port 8080** (from Spring Boot default)
- ✅ **Health Check** (from `/actuator/health`)

## 🎯 **Expected Results:**

✅ **No Docker needed** - Railway handles everything  
✅ **Automatic builds** - On every GitHub push  
✅ **Health monitoring** - Built-in health checks  
✅ **Auto-scaling** - Railway handles traffic  
✅ **SSL/HTTPS** - Automatic SSL certificates  
✅ **Custom domain** - Easy domain setup  

## 🔍 **Troubleshooting:**

### **If build fails:**
1. Check Railway logs in dashboard
2. Verify environment variables
3. Check MongoDB Atlas connection
4. Ensure `pom.xml` is valid

### **If deployment fails:**
1. Check health endpoint: `https://your-app.railway.app/actuator/health`
2. Verify MongoDB Atlas is accessible
3. Check environment variables are set
4. Review Railway logs

## 📝 **Benefits of No-Docker Approach:**

- ✅ **Simpler** - No Docker configuration needed
- ✅ **Faster** - No Docker image building
- ✅ **Automatic** - Railway handles everything
- ✅ **Reliable** - Railway's proven Java support
- ✅ **Scalable** - Automatic scaling

---
**Status**: ✅ **RAILWAY SIMPLE DEPLOYMENT READY**  
**Last Updated**: October 17, 2025  
**Method**: No Docker, Railway auto-detection
