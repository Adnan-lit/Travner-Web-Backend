# Travner Backend - Production Deployment Guide

## Environment Variables Required

Create a `.env` file with the following variables:

```bash
# Database Configuration
MONGODB_URI=mongodb://your-mongodb-host:27017
MONGODB_DATABASE=TravnerDB
MONGO_MAX_CONNECTIONS=50
MONGO_MIN_CONNECTIONS=5
MONGO_MAX_WAIT_TIME=30000
MONGO_CONNECT_TIMEOUT=5000
MONGO_SOCKET_TIMEOUT=30000

# Server Configuration
PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Application Configuration
APP_UPLOAD_DIR=/app/uploads
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

# Security Configuration
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_secure_password_here

# Logging Configuration
LOG_LEVEL=WARN
SECURITY_LOG_LEVEL=WARN
MONGO_LOG_LEVEL=WARN
WEB_LOG_LEVEL=WARN
LOG_FILE_PATH=/app/logs/travner.log

# Chat Configuration
CHAT_MESSAGE_MAX_LENGTH=4000
CHAT_MESSAGE_MAX_ATTACHMENTS=5
CHAT_RATE_LIMIT_MESSAGES=60
CHAT_TYPING_COOLDOWN=2
CHAT_BROKER_TYPE=simple
CHAT_RELAY_HOST=localhost
CHAT_RELAY_PORT=61613
CHAT_RELAY_LOGIN=guest
CHAT_RELAY_PASSCODE=guest
```

## Docker Deployment

1. Build the Docker image:
```bash
docker build -t travner-backend .
```

2. Run with environment variables:
```bash
docker run -d \
  --name travner-backend \
  -p 8080:8080 \
  -e MONGODB_URI=mongodb://your-mongodb-host:27017 \
  -e ADMIN_PASSWORD=your_secure_password \
  -e CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com \
  travner-backend
```

## Manual Deployment

1. Build the application:
```bash
./mvnw clean package -DskipTests
```

2. Run the JAR file:
```bash
java -jar target/Travner-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Health Checks

- Health endpoint: `GET /actuator/health`
- Info endpoint: `GET /actuator/info`
- Metrics endpoint: `GET /actuator/metrics`

## Security Considerations

1. Change default admin password
2. Use HTTPS in production
3. Configure proper CORS origins
4. Set up proper MongoDB authentication
5. Use environment variables for sensitive data
6. Enable proper logging and monitoring
