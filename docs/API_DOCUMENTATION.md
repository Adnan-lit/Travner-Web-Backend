# Travner API Documentation

Complete API reference for the Travner travel blog and social platform.

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Response Format](#api-response-format)
4. [Error Handling](#error-handling)
5. [Public APIs](#public-apis)
6. [User Management APIs](#user-management-apis)
7. [Post Management APIs](#post-management-apis)
8. [Comment APIs](#comment-apis)
9. [Market APIs](#market-apis)
10. [Cart APIs](#cart-apis)
11. [Chat APIs](#chat-apis)
12. [Admin APIs](#admin-apis)
13. [Media Management APIs](#media-management-apis)
14. [WebSocket Chat](#websocket-chat)
15. [Examples](#examples)

---

## Overview

Travner is a comprehensive travel blog and experience sharing platform that allows users to:

- Register and manage their profiles
- Create, share, and discover travel posts
- Upload and manage media files (images/videos) for posts
- Interact through comments and voting
- Browse and purchase travel-related products in the marketplace
- Manage shopping cart and make purchases
- Chat in real-time with other travelers
- Admin users can manage the platform and users

**Base URL**: `http://localhost:8080`  
**API Documentation**: This document  
**Health Check**: `http://localhost:8080/actuator/health`

### Technology Stack
- **Backend**: Spring Boot 3.5.5 with Java 21
- **Database**: MongoDB with GridFS for file storage
- **Security**: Spring Security with Basic Authentication
- **Real-time Communication**: WebSocket for chat functionality

---

## Authentication

**ALL protected endpoints use Basic Authentication consistently.**

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

### User Roles

- **USER**: Standard user role, can manage own content
- **ADMIN**: Administrative role, can manage all users and content

### Example

```bash
# For username 'john' and password 'password123'
echo -n 'john:password123' | base64
# Result: am9objpwYXNzd29yZDEyMw==

# Use in requests:
curl -H "Authorization: Basic am9objpwYXNzd29yZDEyMw==" http://localhost:8080/api/user/profile
```

---

## API Response Format

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

### Pagination Format

```json
{
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "first": true,
    "last": false
  }
}
```

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-10-08T10:30:00.000Z"
}
```

---

## Error Handling

### HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

---

## Public APIs

### Health Check
```http
GET /actuator/health
```
Check if the application is running.

### Public User Information
```http
GET /api/public/user/{username}
```
Get basic public information about a user.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "userName": "john_traveler",
    "firstName": "John",
    "lastName": "Doe",
    "bio": "Travel enthusiast",
    "location": "New York",
    "profileImageUrl": "https://example.com/profile.jpg"
  }
}
```

### Check Username Availability
```http
GET /api/public/check-username/{username}
```
Check if a username is available for registration.

**Response:**
```json
{
  "success": true,
  "message": "Username check completed",
  "data": {
    "available": true
  }
}
```

---

## User Management APIs

### Register New User
```http
POST /api/public/register
Content-Type: application/json

{
  "userName": "john_traveler",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "bio": "Travel enthusiast exploring the world",
  "location": "New York, USA"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "userName": "john_traveler",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "bio": "Travel enthusiast exploring the world",
    "location": "New York, USA"
  }
}
```

### Get User Profile
```http
GET /api/user/profile
Authorization: Basic <credentials>
```

**Response:**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "userName": "john_traveler",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "bio": "Travel enthusiast exploring the world",
    "location": "New York, USA",
    "profileImageUrl": "https://example.com/profile.jpg"
  }
}
```

### Update User Profile
```http
PUT /api/user/profile
Authorization: Basic <credentials>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "newemail@example.com",
  "bio": "Updated bio - passionate traveler",
  "location": "Los Angeles, USA",
  "profileImageUrl": "https://example.com/new-profile.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "userName": "john_traveler",
    "firstName": "John",
    "lastName": "Doe",
    "email": "newemail@example.com",
    "bio": "Updated bio - passionate traveler",
    "location": "Los Angeles, USA",
    "profileImageUrl": "https://example.com/new-profile.jpg"
  }
}
```

### Change Password
```http
PUT /api/user/password
Authorization: Basic <credentials>
Content-Type: application/json

{
  "currentPassword": "oldPassword",
  "newPassword": "newPassword123"
}
```

### Delete Account
```http
DELETE /api/user/account
Authorization: Basic <credentials>
```

---

## Post Management APIs

### Create Post
```http
POST /api/posts
Authorization: Basic <credentials>
Content-Type: application/json

{
  "title": "Amazing Trip to Paris",
  "content": "Detailed travel experience...",
  "location": "Paris, France",
  "tags": ["travel", "europe", "culture"],
  "published": true
}
```

### Get All Published Posts
```http
GET /api/posts?page=0&size=10&sortBy=createdAt&direction=desc
```

### Get Post by ID
```http
GET /api/posts/{postId}
```

### Update Post
```http
PUT /api/posts/{postId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated content...",
  "location": "Updated Location",
  "tags": ["updated", "tags"],
  "published": true
}
```

### Delete Post
```http
DELETE /api/posts/{postId}
Authorization: Basic <credentials>
```

### Vote on Post
```http
POST /api/posts/{postId}/vote
Authorization: Basic <credentials>
Content-Type: application/json

{
  "isUpvote": true
}
```

### Search Posts
```http
GET /api/posts/search?query=paris&page=0&size=10
```

### Get Posts by Location
```http
GET /api/posts/location/{location}?page=0&size=10
```

### Get Posts by Tags
```http
GET /api/posts/tags?tags=travel,europe&page=0&size=10
```

### Get User's Posts
```http
GET /api/posts/user/{username}?page=0&size=10
```

---

## Comment APIs

### Add Comment to Post
```http
POST /api/comments/posts/{postId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "content": "Great post! Thanks for sharing.",
  "parentCommentId": null
}
```

### Get Comments for Post
```http
GET /api/comments/posts/{postId}?page=0&size=10
```

### Update Comment
```http
PUT /api/comments/{commentId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "content": "Updated comment content"
}
```

### Delete Comment
```http
DELETE /api/comments/{commentId}
Authorization: Basic <credentials>
```

### Vote on Comment
```http
POST /api/comments/{commentId}/vote
Authorization: Basic <credentials>
Content-Type: application/json

{
  "isUpvote": true
}
```

---

## Market APIs

### Get All Products
```http
GET /api/market/products?page=0&size=10&sortBy=createdAt&direction=desc
```

### Get Product by ID
```http
GET /api/market/products/{productId}
```

### Create Product
```http
POST /api/market/products
Authorization: Basic <credentials>
Content-Type: application/json

{
  "name": "Travel Backpack",
  "description": "High-quality travel backpack...",
  "price": 89.99,
  "category": "accessories",
  "stockQuantity": 50,
  "location": "New York",
  "tags": ["travel", "backpack", "accessories"],
  "images": ["image1.jpg", "image2.jpg"]
}
```

### Update Product
```http
PUT /api/market/products/{productId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "name": "Updated Product Name",
  "description": "Updated description...",
  "price": 99.99,
  "stockQuantity": 45
}
```

### Delete Product
```http
DELETE /api/market/products/{productId}
Authorization: Basic <credentials>
```

### Search Products
```http
GET /api/market/products/search?query=backpack&page=0&size=10
```

### Get Products by Category
```http
GET /api/market/products/category/{category}?page=0&size=10
```

### Get Products by Location
```http
GET /api/market/products/location/{location}?page=0&size=10
```

### Get Products by Tags
```http
GET /api/market/products/tags?tags=travel,accessories&page=0&size=10
```

### Get Seller's Products
```http
GET /api/market/products/seller/{sellerId}?page=0&size=10
```

---

## Cart APIs

### Get User's Cart
```http
GET /api/cart
Authorization: Basic <credentials>
```

### Add Item to Cart
```http
POST /api/cart/items
Authorization: Basic <credentials>
Content-Type: application/json

{
  "productId": "product123",
  "quantity": 2
}
```

### Update Cart Item Quantity
```http
PUT /api/cart/items/{productId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "quantity": 3
}
```

### Remove Item from Cart
```http
DELETE /api/cart/items/{productId}
Authorization: Basic <credentials>
```

### Clear Cart
```http
DELETE /api/cart
Authorization: Basic <credentials>
```

### Checkout Cart
```http
POST /api/cart/checkout
Authorization: Basic <credentials>
Content-Type: application/json

{
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "paymentMethod": "credit_card"
}
```

---

## Chat APIs

### Get User's Conversations
```http
GET /api/conversations
Authorization: Basic <credentials>
```

### Start New Conversation
```http
POST /api/conversations
Authorization: Basic <credentials>
Content-Type: application/json

{
  "participantUsername": "other_user"
}
```

### Get Conversation Messages
```http
GET /api/conversations/{conversationId}/messages?page=0&size=20
Authorization: Basic <credentials>
```

### Send Message
```http
POST /api/conversations/{conversationId}/messages
Authorization: Basic <credentials>
Content-Type: application/json

{
  "content": "Hello! How are you?"
}
```

### Mark Message as Read
```http
PUT /api/messages/{messageId}/read
Authorization: Basic <credentials>
```

---

## Admin APIs

**Note: All admin endpoints require ADMIN role**

### User Management

#### Get All Users
```http
GET /api/admin/users?page=0&size=20&sortBy=createdAt&direction=desc
Authorization: Basic <admin-credentials>
```

#### Get User by Username
```http
GET /api/admin/users/{username}
Authorization: Basic <admin-credentials>
```

#### Create Admin User
```http
POST /api/admin/users
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "userName": "new_admin",
  "password": "securePassword123",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com"
}
```

#### Update User Roles
```http
PUT /api/admin/users/{username}/roles
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "roles": ["USER", "ADMIN"]
}
```

#### Set User Status
```http
PUT /api/admin/users/{username}/status
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "active": false
}
```

#### Delete User
```http
DELETE /api/admin/users/{username}
Authorization: Basic <admin-credentials>
```

#### Reset User Password
```http
PUT /api/admin/users/{username}/password
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "password": "newPassword123"
}
```

#### Promote User to Admin
```http
POST /api/admin/users/{username}/promote
Authorization: Basic <admin-credentials>
```

#### Get Users by Role
```http
GET /api/admin/users/role/{role}
Authorization: Basic <admin-credentials>
```

### Content Management

#### Get All Posts (including unpublished)
```http
GET /api/admin/posts?page=0&size=20&sortBy=createdAt&direction=desc
Authorization: Basic <admin-credentials>
```

#### Delete Any Post
```http
DELETE /api/admin/posts/{postId}
Authorization: Basic <admin-credentials>
```

#### Get All Products
```http
GET /api/admin/products?page=0&size=20&sortBy=createdAt&direction=desc
Authorization: Basic <admin-credentials>
```

#### Delete Any Product
```http
DELETE /api/admin/products/{productId}
Authorization: Basic <admin-credentials>
```

### Statistics

#### Get System Statistics
```http
GET /api/admin/statistics
Authorization: Basic <admin-credentials>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "users": {
      "total": 1250,
      "admins": 5,
      "regular": 1245,
      "activeUsers": 1250
    },
    "content": {
      "totalPosts": 850,
      "totalProducts": 320,
      "totalComments": 2150,
      "availableProducts": 298
    },
    "timestamp": 1728384600000,
    "serverTime": "2025-10-08T10:30:00"
  }
}
```

---

## Media Management APIs

### Upload Media
```http
POST /api/media/upload
Authorization: Basic <credentials>
Content-Type: multipart/form-data

file: [binary file]
type: post (optional, default: general)
entityId: post123 (optional)
```

### Get Media File by Filename
```http
GET /api/media/files/{filename}
```

### Get Media for Post
```http
GET /api/media/posts/{postId}
```

### Get Media File by ID
```http
GET /api/media/{mediaId}
```

### Delete Media
```http
DELETE /api/media/{mediaId}
Authorization: Basic <credentials>
```

---

## WebSocket Chat

### Connect to WebSocket
```javascript
const socket = new WebSocket('ws://localhost:8080/chat');

// Send message
socket.send(JSON.stringify({
  type: 'CHAT',
  content: 'Hello everyone!',
  sender: 'username'
}));

// Receive messages
socket.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};
```

### Message Types
- `JOIN` - User joins chat
- `LEAVE` - User leaves chat  
- `CHAT` - Regular chat message

---

## Examples

### Complete User Journey Example

1. **Register a new user:**
```bash
curl -X POST http://localhost:8080/api/public/register \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "travel_explorer",
    "password": "securePass123",
    "firstName": "Alex",
    "lastName": "Smith",
    "email": "alex@example.com",
    "bio": "Love exploring new places",
    "location": "San Francisco"
  }'
```

2. **Create a travel post:**
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Basic dHJhdmVsX2V4cGxvcmVyOnNlY3VyZVBhc3MxMjM=" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Amazing Weekend in Tokyo",
    "content": "Just returned from an incredible weekend in Tokyo...",
    "location": "Tokyo, Japan",
    "tags": ["travel", "japan", "culture", "food"],
    "published": true
  }'
```

3. **Upload photos for the post:**
```bash
curl -X POST http://localhost:8080/api/media/upload \
  -H "Authorization: Basic dHJhdmVsX2V4cGxvcmVyOnNlY3VyZVBhc3MxMjM=" \
  -F "file=@tokyo_photo.jpg" \
  -F "type=post" \
  -F "entityId=POST_ID_FROM_STEP_2"
```

4. **Add a product to marketplace:**
```bash
curl -X POST http://localhost:8080/api/market/products \
  -H "Authorization: Basic dHJhdmVsX2V4cGxvcmVyOnNlY3VyZVBhc3MxMjM=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Vintage Tokyo Travel Guide",
    "description": "Rare vintage guide with insider tips",
    "price": 29.99,
    "category": "books",
    "stockQuantity": 1,
    "location": "San Francisco",
    "tags": ["tokyo", "travel", "guide", "vintage"]
  }'
```

5. **Get system statistics (admin only):**
```bash
curl -X GET http://localhost:8080/api/admin/statistics \
  -H "Authorization: Basic YWRtaW46YWRtaW5QYXNzd29yZA=="
```

---

## Rate Limiting

Currently, no rate limiting is implemented, but it's recommended for production deployment.

## CORS Configuration

CORS is configured to allow all origins (`*`) for development. For production, configure specific allowed origins.

## Security Considerations

1. **HTTPS**: Use HTTPS in production
2. **Password Policy**: Implement strong password requirements
3. **Session Management**: Consider implementing session tokens for web clients
4. **Input Validation**: All inputs are validated server-side
5. **SQL Injection**: Using MongoDB with proper query construction prevents injection attacks

---

*Last updated: October 8, 2025*
