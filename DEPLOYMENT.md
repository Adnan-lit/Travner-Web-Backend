# Railway Deployment Guide

## Prerequisites
- GitHub repository with this code
- Railway account
- MongoDB Atlas database

## Deployment Steps

1. **Connect to Railway:**
   - Go to Railway dashboard
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository

2. **Configure Environment Variables:**
   ```
   MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/
   MONGODB_DATABASE=TravnerDB
   PORT=8080
   ```

3. **Deploy:**
   - Railway will automatically detect the Dockerfile
   - Build and deploy the application
   - Health check will run on `/actuator/health`

## Features
- ✅ Java 21 with OpenJDK
- ✅ Maven build process
- ✅ Security with non-root user
- ✅ Health checks
- ✅ Optimized Docker layers
- ✅ Clean repository structure
