# 🌍 Travner - Travel Social Platform Backend

A comprehensive Spring Boot REST API for a travel blogging and social platform with real-time chat capabilities.

## 🚀 Features

- **User Management**: Registration, authentication, profile management
- **Travel Posts**: Create, edit, delete travel blog posts with media
- **Media Management**: Image and file uploads with MongoDB GridFS
- **Comments System**: Nested comments on posts
- **Real-time Chat**: WebSocket-based messaging with conversations
- **Admin Panel**: Administrative functions and user management
- **API Documentation**: Comprehensive REST API documentation

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **Database**: MongoDB
- **Authentication**: Basic Authentication for REST, JWT for WebSocket
- **Real-time**: WebSocket with STOMP protocol
- **Documentation**: OpenAPI/Swagger
- **Build Tool**: Maven

## 📁 Project Structure

```
src/
├── main/java/org/adnan/travner/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers and WebSocket handlers
│   ├── domain/          # Domain entities (conversation, message)
│   ├── dto/             # Data Transfer Objects
│   ├── entry/           # Entity classes
│   ├── exception/       # Custom exceptions
│   ├── repository/      # Data access repositories
│   ├── service/         # Business logic services
│   └── util/            # Utility classes
├── main/resources/
│   └── application.yml  # Application configuration
└── test/                # Test classes

docs/
├── api/                 # API documentation
├── frontend/            # Frontend integration guides
└── testing/             # Testing resources
```

## ⚙️ Setup & Configuration

### Prerequisites

- Java 21
- MongoDB
- Maven (or use included wrapper)
- IntelliJ IDEA (recommended) or any Java IDE

### IDE Setup (IntelliJ IDEA)

The project includes IntelliJ IDEA configuration files in the `.idea/` folder:

- Spring Boot run configurations
- Proper Java 21 and Maven settings
- Lombok annotation processing enabled

1. Open the project folder in IntelliJ IDEA
2. Wait for Maven import to complete
3. Ensure Lombok plugin is installed
4. Use the pre-configured "TravnerApplication" run configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
MONGODB_URI=mongodb://localhost:27017/travner
MONGODB_DATABASE=TravnerDB
JWT_SECRET=your-secret-key-here
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
```

### Running the Application

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or using Maven
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## 📖 Documentation

### API Documentation

- **[Main API Documentation](docs/api/API_DOCUMENTATION.md)** - Complete REST API reference
- **[Chat API Documentation](docs/api/CHAT_API_DOCUMENTATION.md)** - Real-time chat endpoints
- **[Post System Documentation](docs/api/POST_SYSTEM_README.md)** - Posts and media management
- **[API Improvements](docs/api/API_IMPROVEMENTS.md)** - Recent enhancements

### Frontend Integration

- **[Frontend Integration Guide](docs/frontend/FRONTEND_INTEGRATION.md)** - API integration guide
- **[Angular Frontend Prompt](docs/frontend/ANGULAR_FRONTEND_PROMPT.md)** - Angular-specific guidance
- **[Frontend Prompts](docs/frontend/FRONTEND_PROMPTS/)** - Detailed frontend development phases

### Testing

- **[Postman Testing Guide](docs/testing/POSTMAN_TESTING_GUIDE.md)** - API testing with Postman
- **[Postman Collection](docs/testing/travner-social-api-postman.json)** - Ready-to-use Postman collection

## 🔐 Authentication

### REST API

Uses Basic Authentication:

```
Authorization: Basic <base64-encoded-credentials>
```

### WebSocket

Uses JWT authentication:

```
Authorization: Bearer <jwt-token>
```

## 🌐 API Endpoints

### Public Endpoints

- `POST /public/create-user` - User registration
- `GET /public/check-username/{username}` - Username availability
- `GET /posts` - View public posts

### Protected Endpoints

- `GET /user` - User profile
- `POST /posts` - Create post
- `POST /posts/{id}/upvote` - Vote on posts
- `GET /api/chat/conversations` - Chat conversations

## 🔧 Development

### Building

```bash
./mvnw clean install
```

### Testing

```bash
./mvnw test
```

### Code Quality

- Uses Lombok for reducing boilerplate
- Follows Spring Boot best practices
- Comprehensive error handling
- Structured logging

## 🚀 Deployment

The application is ready for deployment on platforms like:

- Railway
- Heroku
- AWS
- Docker

## 📄 License

This project is proprietary software developed for the Travner platform.

## 🤝 Contributing

1. Follow the existing code style
2. Add tests for new features
3. Update documentation as needed
4. Ensure all tests pass before submitting

---

**Happy Coding!** 🌍✈️
