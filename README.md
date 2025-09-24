# Travner Web Backend

A comprehensive travel blog and experience sharing platform built with Spring Boot. This backend provides REST APIs for user management, post creation, community features, and real-time chat functionality.

## 🚀 Features

### Authentication & User Management

- **Basic Authentication** for all protected endpoints
- User registration and profile management
- Enhanced user profiles with bio, location, and profile images
- Admin panel for user management
- Public user profile access

### Community Platform

- **Post Management**: Create, update, delete travel posts with media support
- **Interactive Features**: Comments, upvoting/downvoting system
- **Search & Discovery**: Search posts by content, location, and tags
- **Pagination**: Efficient pagination for all listing endpoints

### Real-Time Chat System

- **WebSocket-based** real-time messaging
- **Conversation Management**: Direct and group conversations
- **Message Features**: Send, edit, delete messages
- **Read Receipts**: Track message read status
- **Typing Indicators**: Real-time typing notifications

### Admin Features

- User management (activate/deactivate, role management)
- System statistics
- Content moderation capabilities

## 🛠 Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **Database**: MongoDB
- **Security**: Spring Security with Basic Authentication
- **WebSocket**: Spring WebSocket with STOMP
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Build Tool**: Maven

## 📋 Prerequisites

- Java 21 or higher
- MongoDB instance
- Maven 3.6 or higher

## ⚡ Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd Travner-Web-Backend
```

### 2. Environment Configuration

Create a `.env` file or set environment variables:

```bash
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=TravnerDB
PORT=8080
```

### 3. Build and Run

```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Start the application
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## 🔐 Authentication

All protected endpoints use **Basic Authentication**:

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

### Example:

```bash
# For username 'john' and password 'password123'
echo -n 'john:password123' | base64
# Result: am9objpwYXNzd29yZDEyMw==

curl -H "Authorization: Basic am9objpwYXNzd29yZDEyMw==" \
     http://localhost:8080/user/profile
```

## 🔌 WebSocket Chat

Connect to WebSocket endpoint: `ws://localhost:8080/ws`

### Message Types

#### Send Message

```json
{
  "type": "SEND_MESSAGE",
  "conversationId": "conv123",
  "content": "Hello!",
  "messageType": "TEXT"
}
```

#### Typing Indicator

```json
{
  "type": "TYPING",
  "conversationId": "conv123",
  "isTyping": true
}
```

## 📊 API Response Format

All API responses follow this consistent format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    /* actual response data */
  },
  "pagination": {
    /* pagination info for paginated responses */
  }
}
```

## 🔧 Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:TravnerDB}

server:
  port: ${PORT:8080}

app:
  upload:
    dir: ${APP_UPLOAD_DIR:${user.home}/travner-uploads}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:3000}
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📚 API Documentation

**Complete API Reference**: [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md)

Interactive API documentation is available at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Quick API Overview

The API provides endpoints for:

- **User Management** - Registration, profiles, authentication
- **Post System** - Create, manage, and discover travel posts
- **Comments** - Interactive commenting system with voting
- **Real-time Chat** - WebSocket-based messaging
- **Admin Panel** - User and content management

All protected endpoints use **Basic Authentication**. See the complete documentation for detailed examples and request/response formats.

## 🛡 Security Features

- **Basic Authentication** for all protected endpoints
- **CORS** configuration for cross-origin requests
- **Role-based access control** (USER, ADMIN)
- **Password encryption** using BCrypt
- **User account activation/deactivation**
- **Input validation** and sanitization

## 🏗 Project Structure

```
src/main/java/org/adnan/travner/
├── TravnerApplication.java          # Main application class
├── config/                          # Configuration classes
│   ├── SpringSecurity.java         # Security configuration
│   ├── WebSocketConfig.java        # WebSocket configuration
│   └── CorsConfig.java             # CORS configuration
├── controller/                      # REST controllers
│   ├── UserController.java         # User management
│   ├── PostController.java         # Post management
│   ├── CommentController.java      # Comment system
│   ├── ConversationController.java # Chat conversations
│   ├── MessageController.java      # Chat messages
│   ├── PublicController.java       # Public endpoints
│   └── AdminController.java        # Admin operations
├── service/                         # Business logic
├── repository/                      # Data access layer
├── dto/                            # Data transfer objects
├── entry/                          # Entity classes
└── exception/                      # Custom exceptions
```

## 🚀 Deployment

### Docker (Optional)

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/Travner-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables for Production

```bash
MONGODB_URI=mongodb://your-mongo-host:27017
MONGODB_DATABASE=TravnerProd
PORT=8080
APP_UPLOAD_DIR=/app/uploads
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For questions or support, please create an issue in the repository or contact the development team.

---

**Built with ❤️ using Spring Boot**
