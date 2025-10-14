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

**Travner uses HTTP Basic Authentication for all protected endpoints.**

Unlike token-based authentication systems, there is no separate login/signin endpoint. Instead, you include your credentials with every request using the Authorization header.

### How Authentication Works

1. **Register** a new account using `/api/public/register`
2. **Authenticate** by including your username and password in the `Authorization` header for protected endpoints
3. The server validates credentials on each request

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

### User Roles

- **USER**: Standard user role, can manage own content
- **ADMIN**: Administrative role, can manage all users and content

### Authentication Example

```bash
# Step 1: Register a new user
curl -X POST http://localhost:8080/api/public/register \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "john",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  }'

# Step 2: Create Base64 credentials
# For username 'john' and password 'password123'
echo -n 'john:password123' | base64
# Result: am9objpwYXNzd29yZDEyMw==

# Step 3: Use credentials in protected requests
curl -H "Authorization: Basic am9objpwYXNzd29yZDEyMw==" \
  http://localhost:8080/api/user/profile
```

### Authentication in Different Clients

#### JavaScript/Fetch API
```javascript
const username = 'john';
const password = 'password123';
const credentials = btoa(`${username}:${password}`);

fetch('http://localhost:8080/api/user/profile', {
  headers: {
    'Authorization': `Basic ${credentials}`
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

#### Postman
1. Go to the **Authorization** tab
2. Select **Basic Auth** from the Type dropdown
3. Enter your **Username** and **Password**
4. Postman will automatically encode and send the credentials

#### cURL
```bash
# Option 1: Using -u flag (automatic encoding)
curl -u john:password123 http://localhost:8080/api/user/profile

# Option 2: Manual header (requires pre-encoded credentials)
curl -H "Authorization: Basic am9objpwYXNzd29yZDEyMw==" \
  http://localhost:8080/api/user/profile
```

### Important Notes

- **No session tokens**: Each request must include credentials
- **HTTPS recommended**: Always use HTTPS in production to protect credentials
- **No separate login endpoint**: Authentication happens automatically with each request
- **No logout needed**: Simply stop sending credentials
- **Password security**: Passwords are securely hashed using BCrypt in the database

### Authentication Errors

**401 Unauthorized** - Invalid credentials or missing Authorization header
```json
{
  "success": false,
  "message": "Authentication required",
  "timestamp": "2025-10-12T10:30:00.000Z"
}
```

**403 Forbidden** - Valid credentials but insufficient permissions
```json
{
  "success": false,
  "message": "Access denied",
  "timestamp": "2025-10-12T10:30:00.000Z"
}
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

**Response:**
```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1a1",
    "title": "Amazing Trip to Paris",
    "content": "Detailed travel experience...",
    "location": "Paris, France",
    "tags": ["travel", "europe", "culture"],
    "authorId": "507f1f77bcf86cd799439011",
    "authorUsername": "john_traveler",
    "published": true,
    "upvotes": 0,
    "downvotes": 0,
    "createdAt": "2025-10-11T10:30:00.000Z",
    "updatedAt": "2025-10-11T10:30:00.000Z"
  }
}
```

### Get All Published Posts
```http
GET /api/posts?page=0&size=10&sortBy=createdAt&direction=desc
```

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size
- `sortBy` (optional, default: createdAt) - Field to sort by
- `direction` (optional, default: desc) - Sort direction (asc or desc)

**Response:**
```json
{
  "success": true,
  "message": "Posts retrieved successfully",
  "data": [
    {
      "id": "60d5ec49f1b2c8b1f8e4e1a1",
      "title": "Amazing Trip to Paris",
      "content": "Detailed travel experience...",
      "location": "Paris, France",
      "tags": ["travel", "europe", "culture"],
      "authorUsername": "john_traveler",
      "upvotes": 15,
      "downvotes": 2,
      "commentCount": 8,
      "createdAt": "2025-10-11T10:30:00.000Z"
    }
  ],
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

### Get Post by ID
```http
GET /api/posts/{postId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1a1",
    "title": "Amazing Trip to Paris",
    "content": "Detailed travel experience...",
    "location": "Paris, France",
    "tags": ["travel", "europe", "culture"],
    "authorId": "507f1f77bcf86cd799439011",
    "authorUsername": "john_traveler",
    "published": true,
    "upvotes": 15,
    "downvotes": 2,
    "commentCount": 8,
    "createdAt": "2025-10-11T10:30:00.000Z",
    "updatedAt": "2025-10-11T10:30:00.000Z"
  }
}
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

**Note:** Only the post author can update their own posts.

### Delete Post
```http
DELETE /api/posts/{postId}
Authorization: Basic <credentials>
```

**Note:** Only the post author can delete their own posts (admins can delete any post via admin endpoint).

### Vote on Post (Unified Endpoint)
```http
POST /api/posts/{postId}/vote
Authorization: Basic <credentials>
Content-Type: application/json

{
  "isUpvote": true
}
```

**Request Body:**
- `isUpvote` (required, boolean) - `true` for upvote, `false` for downvote

**Response:**
```json
{
  "success": true,
  "message": "Post upvoted successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1a1",
    "title": "Amazing Trip to Paris",
    "upvotes": 16,
    "downvotes": 2
  }
}
```

### Upvote Post (Alternative Endpoint)
```http
POST /api/posts/{postId}/upvote
Authorization: Basic <credentials>
```

### Downvote Post (Alternative Endpoint)
```http
POST /api/posts/{postId}/downvote
Authorization: Basic <credentials>
```

### Search Posts
```http
GET /api/posts/search?query=paris&page=0&size=10
```

**Query Parameters:**
- `query` (required) - Search term (searches in title, content, location, and tags)
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

**Response:**
```json
{
  "success": true,
  "message": "Posts found",
  "data": [
    {
      "id": "60d5ec49f1b2c8b1f8e4e1a1",
      "title": "Amazing Trip to Paris",
      "content": "Travel experience in Paris...",
      "location": "Paris, France",
      "tags": ["travel", "europe", "culture"]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

### Get Posts by Location
```http
GET /api/posts/location/{location}?page=0&size=10
```

**Path Parameters:**
- `location` (required) - Location name (can be partial match, e.g., "Paris" matches "Paris, France")

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

**Example:**
```bash
curl http://localhost:8080/api/posts/location/Paris?page=0&size=10
```

### Get Posts by Tags
```http
GET /api/posts/tags?tags=travel,europe&page=0&size=10
```

**Query Parameters:**
- `tags` (required) - Comma-separated list of tags
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

**Note:** Returns posts that have ANY of the specified tags.

### Get User's Posts
```http
GET /api/posts/user/{username}?page=0&size=10
```

**Path Parameters:**
- `username` (required) - Username of the post author

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

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

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size
- `sortBy` (optional, default: createdAt) - Field to sort by (createdAt, price, name)
- `direction` (optional, default: desc) - Sort direction (asc or desc)

**Response:**
```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": "60d5ec49f1b2c8b1f8e4e1b1",
      "name": "Travel Backpack",
      "description": "High-quality travel backpack...",
      "price": 89.99,
      "category": "accessories",
      "stockQuantity": 50,
      "location": "New York",
      "tags": ["travel", "backpack", "accessories"],
      "images": ["image1.jpg", "image2.jpg"],
      "sellerId": "507f1f77bcf86cd799439011",
      "sellerUsername": "john_traveler",
      "isAvailable": true,
      "createdAt": "2025-10-11T10:30:00.000Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 320,
    "totalPages": 32
  }
}
```

### Get Product by ID
```http
GET /api/market/products/{productId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1b1",
    "name": "Travel Backpack",
    "description": "High-quality travel backpack with multiple compartments...",
    "price": 89.99,
    "category": "accessories",
    "stockQuantity": 50,
    "location": "New York",
    "tags": ["travel", "backpack", "accessories"],
    "images": ["image1.jpg", "image2.jpg"],
    "sellerId": "507f1f77bcf86cd799439011",
    "sellerUsername": "john_traveler",
    "isAvailable": true,
    "createdAt": "2025-10-11T10:30:00.000Z",
    "updatedAt": "2025-10-11T10:30:00.000Z"
  }
}
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

**Response:**
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1b1",
    "name": "Travel Backpack",
    "price": 89.99,
    "sellerId": "507f1f77bcf86cd799439011",
    "sellerUsername": "john_traveler",
    "isAvailable": true,
    "createdAt": "2025-10-11T10:30:00.000Z"
  }
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

**Note:** Only the product seller can update their own products.

### Delete Product
```http
DELETE /api/market/products/{productId}
Authorization: Basic <credentials>
```

**Note:** Only the product seller can delete their own products (admins can delete any product via admin endpoint).

### Search Products
```http
GET /api/market/products/search?query=backpack&page=0&size=10
```

**Query Parameters:**
- `query` (required) - Search term (searches in name, description, category, and tags)
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

### Get Products by Category
```http
GET /api/market/products/category/{category}?page=0&size=10
```

**Path Parameters:**
- `category` (required) - Category name (e.g., "accessories", "books", "gear")

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

**Example:**
```bash
curl http://localhost:8080/api/market/products/category/accessories?page=0&size=10
```

### Get Products by Location
```http
GET /api/market/products/location/{location}?page=0&size=10
```

**Path Parameters:**
- `location` (required) - Location name (can be partial match)

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

### Get Products by Tags
```http
GET /api/market/products/tags?tags=travel,accessories&page=0&size=10
```

**Query Parameters:**
- `tags` (required) - Comma-separated list of tags
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

### Get Seller's Products
```http
GET /api/market/products/seller/{sellerId}?page=0&size=10
```

**Path Parameters:**
- `sellerId` (required) - Seller's user ID or username

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 10) - Page size

---

## Cart APIs

### Get User's Cart
```http
GET /api/cart
Authorization: Basic <credentials>
```

**Response:**
```json
{
  "success": true,
  "message": "Cart retrieved successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1c1",
    "userId": "john_traveler",
    "items": [
      {
        "productId": "60d5ec49f1b2c8b1f8e4e1b1",
        "productName": "Travel Backpack",
        "unitPrice": 89.99,
        "quantity": 2,
        "subtotal": 179.98,
        "sellerId": "507f1f77bcf86cd799439011",
        "sellerName": "seller_username",
        "productImage": "image1.jpg",
        "addedAt": "2025-10-11T10:30:00.000Z"
      }
    ],
    "totalAmount": 179.98,
    "totalItems": 2,
    "createdAt": "2025-10-11T09:00:00.000Z",
    "updatedAt": "2025-10-11T10:30:00.000Z"
  }
}
```

### Add Item to Cart
```http
POST /api/cart/items
Authorization: Basic <credentials>
Content-Type: application/json

{
  "productId": "60d5ec49f1b2c8b1f8e4e1b1",
  "quantity": 2
}
```

**Request Body:**
- `productId` (required, string) - ID of the product to add
- `quantity` (required, integer) - Quantity to add (must be greater than 0)

**Response:**
```json
{
  "success": true,
  "message": "Item added to cart successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1c1",
    "userId": "john_traveler",
    "items": [
      {
        "productId": "60d5ec49f1b2c8b1f8e4e1b1",
        "productName": "Travel Backpack",
        "quantity": 2,
        "subtotal": 179.98
      }
    ],
    "totalAmount": 179.98,
    "totalItems": 2
  }
}
```

**Error Cases:**
- Product not found (404)
- Product not available (400)
- Insufficient stock (400)

### Update Cart Item Quantity
```http
PUT /api/cart/items/{productId}
Authorization: Basic <credentials>
Content-Type: application/json

{
  "quantity": 3
}
```

**Path Parameters:**
- `productId` (required) - ID of the product in the cart

**Request Body:**
- `quantity` (required, integer) - New quantity (0 to remove the item, >0 to update)

**Response:**
```json
{
  "success": true,
  "message": "Cart item updated successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1c1",
    "userId": "john_traveler",
    "items": [
      {
        "productId": "60d5ec49f1b2c8b1f8e4e1b1",
        "productName": "Travel Backpack",
        "quantity": 3,
        "subtotal": 269.97
      }
    ],
    "totalAmount": 269.97,
    "totalItems": 3
  }
}
```

**Note:** Setting quantity to 0 removes the item from the cart.

### Remove Item from Cart
```http
DELETE /api/cart/items/{productId}
Authorization: Basic <credentials>
```

**Path Parameters:**
- `productId` (required) - ID of the product to remove

**Response:**
```json
{
  "success": true,
  "message": "Item removed from cart successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1c1",
    "userId": "john_traveler",
    "items": [],
    "totalAmount": 0.00,
    "totalItems": 0
  }
}
```

### Clear Cart
```http
DELETE /api/cart/clear
Authorization: Basic <credentials>
```

**Response:**
```json
{
  "success": true,
  "message": "Cart cleared successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1c1",
    "userId": "john_traveler",
    "items": [],
    "totalAmount": 0.00,
    "totalItems": 0
  }
}
```

### Get Cart Item Count
```http
GET /api/cart/count
Authorization: Basic <credentials>
```

**Response:**
```json
{
  "success": true,
  "message": "Cart item count retrieved successfully",
  "data": 5
}
```

### Checkout Cart
```http
POST /api/cart/checkout
Authorization: Basic <credentials>
```

**Response:**
```json
{
  "success": true,
  "message": "Checkout successful",
  "data": {
    "message": "Order placed successfully",
    "totalAmount": 269.97,
    "itemCount": 3
  }
}
```

**Error Cases:**
- Cart is empty (400)
- Product no longer available (400)
- Insufficient stock (400)

**Note:** This is a simplified checkout endpoint. In production, you would typically send shipping address and payment information in the request body.

---

## Chat APIs

### Get User's Conversations
```http
GET /api/conversations?page=0&size=20
Authorization: Basic <credentials>
```

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 20) - Page size

**Response:**
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": [
    {
      "id": "60d5ec49f1b2c8b1f8e4e1d1",
      "type": "DIRECT",
      "name": null,
      "participants": [
        {
          "userId": "507f1f77bcf86cd799439011",
          "username": "john_traveler"
        },
        {
          "userId": "507f1f77bcf86cd799439012",
          "username": "jane_explorer"
        }
      ],
      "lastMessage": "Hello! How are you?",
      "lastMessageAt": "2025-10-11T10:30:00.000Z",
      "unreadCount": 2,
      "createdAt": "2025-10-10T09:00:00.000Z"
    }
  ]
}
```

### Start New Conversation
```http
POST /api/conversations
Authorization: Basic <credentials>
Content-Type: application/json

{
  "type": "DIRECT",
  "memberIds": ["jane_explorer"]
}
```

**Request Body:**
- `type` (required, string) - Conversation type: "DIRECT" or "GROUP"
- `memberIds` (required, array) - Array of usernames or user IDs to add to conversation
- `name` (optional, string) - Group name (required for GROUP type)

**Response:**
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1d1",
    "type": "DIRECT",
    "participants": [
      {
        "userId": "507f1f77bcf86cd799439011",
        "username": "john_traveler"
      },
      {
        "userId": "507f1f77bcf86cd799439012",
        "username": "jane_explorer"
      }
    ],
    "createdAt": "2025-10-11T10:30:00.000Z"
  }
}
```

**Note:** For DIRECT conversations, if a conversation already exists between the users, it will return the existing conversation instead of creating a new one.

### Get Conversation Messages
```http
GET /api/conversations/{conversationId}/messages?page=0&size=20
Authorization: Basic <credentials>
```

**Path Parameters:**
- `conversationId` (required) - ID of the conversation

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 20) - Page size

**Response:**
```json
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "id": "60d5ec49f1b2c8b1f8e4e1e1",
      "conversationId": "60d5ec49f1b2c8b1f8e4e1d1",
      "senderId": "507f1f77bcf86cd799439011",
      "senderUsername": "john_traveler",
      "kind": "TEXT",
      "body": "Hello! How are you?",
      "attachments": [],
      "replyTo": null,
      "createdAt": "2025-10-11T10:30:00.000Z",
      "readBy": [
        {
          "userId": "507f1f77bcf86cd799439011",
          "readAt": "2025-10-11T10:30:00.000Z"
        }
      ]
    }
  ]
}
```

**Error Cases:**
- Conversation not found (404)
- User not a member of conversation (403)

### Send Message
```http
POST /api/conversations/{conversationId}/messages
Authorization: Basic <credentials>
Content-Type: application/json

{
  "content": "Hello! How are you?",
  "kind": "TEXT",
  "replyToMessageId": null,
  "attachments": []
}
```

**Path Parameters:**
- `conversationId` (required) - ID of the conversation

**Request Body:**
- `content` (required, string) - Message text content
- `kind` (optional, string, default: "TEXT") - Message type: "TEXT", "IMAGE", "FILE", etc.
- `replyToMessageId` (optional, string) - ID of message being replied to
- `attachments` (optional, array) - Array of attachment objects with mediaId and caption

**Response:**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": "60d5ec49f1b2c8b1f8e4e1e1",
    "conversationId": "60d5ec49f1b2c8b1f8e4e1d1",
    "senderId": "507f1f77bcf86cd799439011",
    "senderUsername": "john_traveler",
    "kind": "TEXT",
    "body": "Hello! How are you?",
    "createdAt": "2025-10-11T10:30:00.000Z"
  }
}
```

**Error Cases:**
- Conversation not found (404)
- User not a member of conversation (403)
- Empty message content (400)

### Mark Messages as Read
```http
PUT /api/conversations/{conversationId}/read?lastReadMessageId={messageId}
Authorization: Basic <credentials>
```

**Path Parameters:**
- `conversationId` (required) - ID of the conversation

**Query Parameters:**
- `lastReadMessageId` (optional) - ID of the last message read (marks all messages up to this one as read)

**Response:**
```json
{
  "success": true,
  "message": "Messages marked as read",
  "data": null
}
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

**Query Parameters:**
- `page` (optional, default: 0) - Zero-based page index
- `size` (optional, default: 20) - Page size
- `sortBy` (optional, default: createdAt) - Field to sort by
- `direction` (optional, default: desc) - Sort direction (asc or desc)

**Response:**
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": "507f1f77bcf86cd799439011",
      "userName": "john_traveler",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "roles": ["USER"],
      "active": true,
      "createdAt": "2025-10-01T10:00:00.000Z"
    }
  ]
}
```
