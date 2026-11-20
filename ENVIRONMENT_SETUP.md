# Environment Setup Guide

## Required Environment Variables

The application requires the following environment variables to be set:

### **MongoDB Configuration** (REQUIRED)
```
MONGODB_URI=mongodb+srv://<username>:<password>@<cluster-name>.mongodb.net/?retryWrites=true&w=majority
MONGODB_DATABASE=TravnerDB
```

### **Application Configuration**
```
PORT=8080
APP_UPLOAD_DIR=<path-to-upload-directory>
```

### **Bootstrap Admin** (Optional - one-time, only when no admin exists)
```
APP_BOOTSTRAP_ADMIN_USERNAME=admin
APP_BOOTSTRAP_ADMIN_PASSWORD=<generate-a-strong-12+-char-password>
APP_BOOTSTRAP_ADMIN_EMAIL=admin@travner.local
```
There is no default admin account. Leave these unset to create none. The
password must contain upper/lower case, a digit and a symbol; it is never
logged. Rotate it after the first login. See `env.example` for all variables.

---

## Setup Methods

### Option 1: IntelliJ IDEA Environment Variables (Recommended)

1. Open **Run/Debug Configurations** (top right dropdown → **Edit Configurations**)
2. Select **TravnerApplication**
3. In **Environment variables** field, add:
   ```
   MONGODB_URI=your_mongodb_uri;MONGODB_DATABASE=TravnerDB;PORT=8080
   ```
   ⚠️ Use semicolon (`;`) to separate multiple variables on Windows

### Option 2: System Environment Variables (For Terminal)

**PowerShell:**
```powershell
$env:MONGODB_URI="your_mongodb_uri"
$env:MONGODB_DATABASE="TravnerDB"
$env:PORT="8080"
```

**Command Prompt:**
```cmd
set MONGODB_URI=your_mongodb_uri
set MONGODB_DATABASE=TravnerDB
set PORT=8080
```

### Option 3: application-local.yml (Not recommended for sensitive data)

Create `src/main/resources/application-local.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://<username>:<password>@<cluster-name>.mongodb.net/?retryWrites=true&w=majority
      database: TravnerDB
```

Then run with:
```bash
mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Verifying Environment Variables

**Check if MongoDB URI is set:**
```powershell
echo $env:MONGODB_URI
```

**Start backend with environment variables in PowerShell:**
```powershell
cd "D:\Travner V2\Travner-Web-Backend"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
$env:MONGODB_URI="your_mongodb_atlas_uri"
$env:MONGODB_DATABASE="TravnerDB"
.\mvnw.cmd spring-boot:run
```

---

## Quick Start

1. **Set MongoDB URI in IntelliJ**:
   - Top right → Edit Configurations → TravnerApplication
   - Environment variables: `MONGODB_URI=<your_uri>;MONGODB_DATABASE=TravnerDB`

2. **Click Run** ▶️

That's it! The backend will start on port 8080.
