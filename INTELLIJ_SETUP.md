# IntelliJ IDEA Setup Instructions

## 🎯 Project Clean-up Summary
The Travner project has been optimized for IntelliJ IDEA with:
- ✅ Simplified MongoDB Atlas configuration
- ✅ Cleaned POM file (removed unnecessary sections)
- ✅ Optimized test configurations
- ✅ All dependencies properly configured

## 🚀 IntelliJ IDEA Setup

### 1. Import Project
1. Open IntelliJ IDEA
2. File → Open → Select project directory
3. Choose "Import project from external model" → "Maven"
4. Click "Import"

### 2. Project Configuration
- **JDK**: Java 21 (configured automatically)
- **Maven**: Uses wrapper (./mvnw.cmd)
- **Spring Boot**: 3.5.5
- **Database**: MongoDB Atlas

### 3. Required IntelliJ Plugins
- **Lombok Plugin** (for @Data, @NoArgsConstructor, etc.)
- **Spring Boot** (usually pre-installed)

### 4. Enable Annotation Processing
1. File → Settings → Build → Compiler → Annotation Processors
2. Check "Enable annotation processing"
3. Apply → OK

### 5. Run Configurations

#### Run Application
- **Main Class**: `org.adnan.travner.TravnerApplication`
- **VM Options**: `-Dspring.profiles.active=default`
- **Environment Variables**: 
  ```
  MONGODB_URI=your_mongodb_atlas_uri
  MONGODB_DATABASE=TravnerDB
  PORT=8080
  ```

#### Run Tests
- **Test Pattern**: `org.adnan.travner.*`
- **VM Options**: `-Dspring.profiles.active=test`

## 📁 Clean Project Structure

```
src/
├── main/
│   ├── java/org/adnan/travner/
│   │   ├── config/
│   │   │   ├── CorsConfig.java
│   │   │   └── SpringSecurity.java
│   │   ├── controller/
│   │   │   ├── AdminController.java
│   │   │   ├── PublicController.java
│   │   │   └── UserController.java
│   │   ├── entry/
│   │   │   └── UserEntry.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── UserDetailsServiceImpl.java
│   │   │   └── UserService.java
│   │   └── TravnerApplication.java
│   └── resources/
│       └── application.yml (MongoDB Atlas only)
└── test/
    ├── java/org/adnan/travner/
    │   ├── controller/
    │   ├── integration/
    │   └── TravnerApplicationTests.java
    └── resources/
        └── application-test.yml (Embedded MongoDB)
```

## ⚙️ Configuration Files

### application.yml (Production)
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:TravnerDB}
      auto-index-creation: true

server:
  port: ${PORT:8080}
  address: 0.0.0.0
```

### application-test.yml (Testing)
```yaml
spring:
  data:
    mongodb:
      database: testdb
  mongodb:
    embedded:
      version: 6.0.6

logging:
  level:
    de.flapdoodle.embed.mongo: WARN
    
server:
  port: 0
```

## 🔧 Maven Commands

```bash
# Compile
./mvnw.cmd compile

# Run application
./mvnw.cmd spring-boot:run

# Run tests
./mvnw.cmd test

# Package
./mvnw.cmd package

# Clean and compile
./mvnw.cmd clean compile
```

## 🔍 Troubleshooting

### Common Issues

1. **Lombok not working**
   - Enable annotation processing in settings
   - Install Lombok plugin

2. **MongoDB connection issues**
   - Check .env file has correct MONGODB_URI
   - Verify MongoDB Atlas credentials

3. **Tests failing**
   - Ensure embedded MongoDB dependency is present
   - Check test profile is active

### IntelliJ-specific Tips

1. **Refresh Maven Project**
   - View → Tool Windows → Maven
   - Click refresh button

2. **Invalidate Caches**
   - File → Invalidate Caches and Restart

3. **Project Structure**
   - Ensure JDK 21 is selected
   - Language level should be "21"

## ✅ Verification Checklist

- [ ] Project imports without errors
- [ ] Lombok annotations work
- [ ] Application runs on port 8080
- [ ] Tests pass (unit and integration)
- [ ] MongoDB Atlas connection works
- [ ] CORS configuration active
- [ ] All API endpoints accessible

Your Travner project is now optimally configured for IntelliJ IDEA development! 🚀