# Travner Web Backend

A comprehensive travel blog and experience sharing platform built with Spring Boot. This backend provides REST APIs for user management, post creation, community features, marketplace, and real-time chat functionality.

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

### Marketplace

- **Product Listings**: Create and manage travel-related products
- **Search & Filter**: By category, location, tags, and seller
- **Shopping Cart**: Full cart management with quantity updates
- **Orders**: Complete order workflow with shipping and payment details

### Real-Time Chat System

- **WebSocket-based** real-time messaging
- **Conversation Management**: Direct and group conversations
- **Message Features**: Send, edit, delete messages
- **Read Receipts**: Track message read status
- **Typing Indicators**: Real-time typing notifications

### Media Management

- **GridFS Storage**: Secure file storage for images and videos
- **Upload Support**: Multipart file upload (max 20MB)
- **Post Attachments**: Link media files to posts

### Admin Features

- User management (activate/deactivate, role management)
- System statistics
- Content moderation capabilities

## 🛠 Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **Database**: MongoDB with GridFS for media storage
- **Security**: Spring Security with Basic Authentication
- **WebSocket**: Spring WebSocket with STOMP
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Build Tool**: Maven

## 📋 Prerequisites

- Java 21 or higher
- MongoDB instance (local or cloud)
- Maven 3.6 or higher

## ⚡ Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd Travner-Web-Backend
```

### 2. Environment Configuration

Create a `.env` file in the root directory:

```env
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=TravnerDB
PORT=8080
APP_UPLOAD_DIR=/path/to/uploads
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

### 3. Build and Run

```bash
# Build the project
mvnw clean compile

# Run tests
mvnw test

# Start the application
mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## 📚 Documentation

### Complete Documentation Files

- **[API_REFERENCE.md](API_REFERENCE.md)** - Complete API reference with all endpoints, request/response formats, authentication, WebSocket, and error codes
- **[FRONTEND_GUIDE.md](FRONTEND_GUIDE.md)** - Comprehensive frontend implementation guide with module-wise code examples for React/Angular/Vue

### Interactive API Documentation

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Health Check**: `http://localhost:8080/actuator/health`

### Quick API Overview

**Public Endpoints** (No authentication required):

- `POST /api/public/register` - Register new user
- `GET /api/public/user/{username}` - Get public user profile
- `GET /api/public/check-username/{username}` - Check username availability
- `GET /api/posts` - Browse posts
- `GET /api/market/products` - Browse marketplace

**Protected Endpoints** (Basic Auth required):

- `/api/user/*` - User profile management
- `/api/posts` - Create/update/delete posts
- `/api/posts/{postId}/comments` - Comment management (legacy: `/api/comments/posts/{postId}`)
- `/api/cart` - Shopping cart operations
- `/api/orders` - Order management
- `/api/chat/conversations` - Chat conversations (legacy: `/api/conversations`)
- `/api/media/upload` - Media upload

**Admin Endpoints** (Admin role required):

- `/api/admin/*` - User and content management

## 🔐 Authentication

Travner uses **HTTP Basic Authentication**. Include credentials with every request:

```bash
# Encode credentials
echo -n 'username:password' | base64

# Use in requests
curl -H "Authorization: Basic <base64-credentials>" \
  http://localhost:8080/api/user/profile
```

**JavaScript Example:**

```javascript
const credentials = btoa(`${username}:${password}`);
fetch('/api/user/profile', {
  headers: { 'Authorization': `Basic ${credentials}` }
});
```

## 🔌 WebSocket Chat

Connect to WebSocket endpoint: `ws://localhost:8080/ws`

**JavaScript Example:**

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // Subscribe to messages
  stompClient.subscribe('/user/queue/messages', function(message) {
    console.log('New message:', JSON.parse(message.body));
  });
});

// Send message
stompClient.send('/app/chat.send', {}, JSON.stringify({
  type: 'SEND_MESSAGE',
  conversationId: 'conv123',
  content: 'Hello!',
  messageType: 'TEXT'
}));
```

## 📊 API Response Format

All API responses follow this consistent format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* response data */ },
  "pagination": { /* for paginated responses */ }
}
```

## 🧪 Testing

The project includes comprehensive integration tests for cart, orders, marketplace, and security.

```bash
# Run all tests
mvnw test

# Run specific test
mvnw test -Dtest=CartIntegrationTest

# Run with coverage
mvnw test jacoco:report
```

## 🏗 Project Structure

```
src/main/java/org/adnan/travner/
├── config/                      # Configuration classes
│   ├── SpringSecurity.java     # Security configuration
│   ├── WebSocketConfig.java    # WebSocket configuration
│   └── CorsConfig.java         # CORS configuration
├── controller/                  # REST controllers
│   ├── UserController.java
│   ├── PostController.java
│   ├── CommentController.java
│   ├── MarketController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── ConversationController.java
│   ├── MessageController.java
│   ├── MediaController.java
│   └── AdminController.java
├── service/                     # Business logic
├── repository/                  # Data access layer
├── dto/                        # Data transfer objects
├── domain/                     # Domain models
└── exception/                  # Custom exceptions
```

## 🚀 Deployment

### Environment Variables for Production

```env
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/
MONGODB_DATABASE=TravnerProd
PORT=8080
APP_UPLOAD_DIR=/app/uploads
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Docker Deployment

```bash
# Build JAR
mvnw clean package -DskipTests

# Run with Docker
docker build -t travner-backend .
docker run -p 8080:8080 --env-file .env travner-backend
```

## 🎯 Key Features Implemented

✅ User authentication and authorization  
✅ Post creation with media support  
✅ Comments and voting system  
✅ Marketplace with products  
✅ Shopping cart and orders  
✅ Real-time chat with WebSocket  
✅ Media file upload (GridFS)  
✅ Admin panel  
✅ Search and filtering  
✅ Pagination  
✅ OpenAPI documentation  
✅ CORS configuration  
✅ Actuator health checks  

## 🔧 Configuration

### CORS

Configured origins (customizable via environment):

- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `https://travner.vercel.app` (Production)

### File Upload

- Max file size: 20MB
- Max request size: 25MB
- Supported formats: Images, videos

### Rate Limiting

- Bucket4j configured for API rate limiting
- WebSocket message rate limiting

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For questions or support:

- Check the [API Reference](API_REFERENCE.md)
- Check the [Frontend Guide](FRONTEND_GUIDE.md)
- Open an issue in the repository
- Consult Swagger UI at `/swagger-ui.html`

---

**Built with ❤️ for travelers by travelers**
