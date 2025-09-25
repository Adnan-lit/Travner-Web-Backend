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
9. [Chat APIs](#chat-apis)
10. [Admin APIs](#admin-apis)
11. [Media Management APIs](#media-management-apis)
12. [WebSocket Chat](#websocket-chat)
13. [Examples](#examples)

---

## Overview

Travner is a comprehensive travel blog and experience sharing platform that allows users to:

- Register and manage their profiles
- Create, share, and discover travel posts
- Upload and manage media files (images/videos) for posts
- Interact through comments and voting
- Chat in real-time with other travelers
- Admin users can manage the platform and users

**Base URL**: `http://localhost:8080`  
**API Documentation**: `http://localhost:8080/swagger-ui.html`  
**Health Check**: `http://localhost:8080/actuator/health`

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
curl -H "Authorization: Basic am9objpwYXNzd29yZDEyMw==" http://localhost:8080/user/profile
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

---

## Error Handling

### HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (authentication required)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `500` - Internal Server Error

### Error Response Format

```json
{
  "success": false,
  "message": "Detailed error description",
  "data": null
}
```

---

## Public APIs

These endpoints do not require authentication.

### User Registration

#### Register New User

```
POST /public/create-user
```

**Request Body:**

```json
{
  "userName": "johndoe",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

**Response:** `201 Created` on success

#### Check Username Availability

```
GET /public/check-username/{username}
```

**Response:**

```json
{
  "message": "Username is available",
  "available": true
}
```

#### Password Reset Flow

```
POST /public/forgot-password
```

**Request Body:**

```json
{
  "username": "johndoe"
}
```

```
POST /public/reset-password
```

**Request Body:**

```json
{
  "token": "reset-token-here",
  "newPassword": "newpassword123"
}
```

#### Create First Admin

```
POST /public/create-first-admin
```

**Request Body:**

```json
{
  "userName": "admin",
  "password": "adminpass123",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com"
}
```

_Note: Only works if no admin users exist in the system._

### Public Profile Access

#### Get Public User Profile

```
GET /user/public/{username}
```

**Response:**

```json
{
  "userName": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "bio": "Travel enthusiast",
  "location": "New York, USA",
  "profileImageUrl": "/uploads/profile.jpg",
  "createdAt": "2025-01-15T10:30:00"
}
```

### Public Post Access

#### Get All Posts

```
GET /posts?page=0&size=10&sortBy=createdAt&direction=desc
```

**Query Parameters:**

- `page` - Page number (default: 0)
- `size` - Page size (default: 10)
- `sortBy` - Sort field (default: createdAt)
- `direction` - Sort direction: asc/desc (default: desc)

#### Get Specific Post

```
GET /posts/{postId}
```

#### Get Posts by User

```
GET /posts/user/{username}?page=0&size=10
```

#### Search Posts

```
GET /posts/search?query=paris&page=0&size=10
```

#### Get Posts by Location

```
GET /posts/location?location=Paris&page=0&size=10
```

#### Get Posts by Tags

```
GET /posts/tags?tags=travel,europe&page=0&size=10
```

#### Get Post Comments

```
GET /posts/{postId}/comments?page=0&size=10
```

---

## User Management APIs

**Authentication Required:** Basic Auth

### Profile Management

#### Get Current User Info

```
GET /user
```

#### Get User Profile

```
GET /user/profile
```

#### Update Full Profile

```
PUT /user/profile
```

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "bio": "Passionate traveler exploring the world",
  "location": "New York, USA"
}
```

#### Partial Profile Update

```
PATCH /user/profile
```

**Request Body:** (any subset of profile fields)

```json
{
  "bio": "Updated bio text",
  "location": "San Francisco, USA"
}
```

#### Change Password

```
PUT /user/password
```

**Request Body:**

```json
{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword123"
}
```

#### Delete Account

```
DELETE /user
```

---

## Post Management APIs

**Authentication Required:** Basic Auth

### Post Operations

#### Create New Post

```
POST /posts
```

**Request Body:**

```json
{
  "title": "Amazing Trip to Tokyo",
  "content": "Tokyo was incredible! Here's my experience...",
  "location": "Tokyo, Japan",
  "tags": ["travel", "japan", "tokyo", "culture"],
  "published": true
}
```

#### Update Post

```
PUT /posts/{postId}
```

**Request Body:** Same as create post

#### Delete Post

```
DELETE /posts/{postId}
```

### Post Interactions

#### Upvote Post

```
POST /posts/{postId}/upvote
```

#### Downvote Post

```
POST /posts/{postId}/downvote
```

---

## Comment APIs

**Authentication Required:** Basic Auth for write operations

### Comment Operations

#### Create Comment

```
POST /posts/{postId}/comments
```

**Request Body:**

```json
{
  "content": "Great post! I love Tokyo too.",
  "parentCommentId": null
}
```

#### Update Comment

```
PUT /posts/{postId}/comments/{commentId}
```

**Request Body:**

```json
{
  "content": "Updated comment content"
}
```

#### Delete Comment

```
DELETE /posts/{postId}/comments/{commentId}
```

### Comment Interactions

#### Upvote Comment

```
POST /posts/{postId}/comments/{commentId}/upvote
```

#### Downvote Comment

```
POST /posts/{postId}/comments/{commentId}/downvote
```

---

## Chat APIs

**Authentication Required:** Basic Auth

### Conversation Management (DIRECT one-to-one only)

#### Get User Conversations

```
GET /api/chat/conversations?page=0&size=20
Authorization: Basic <base64-credentials>
```

**Response:**

```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": {
    "content": [
      {
        "id": "conv123",
        "type": "DIRECT",
        "title": "Travel Chat",
        "memberCount": 2,
        "lastMessage": {
          "content": "Hello there!",
          "senderName": "John Doe",
          "createdAt": "2025-01-15T14:30:00Z"
        },
        "unreadCount": 3,
        "createdAt": "2025-01-15T14:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 5
  }
}
```

#### Create DIRECT Conversation

```
POST /api/chat/conversations
Authorization: Basic <base64-credentials>
Content-Type: application/json
```

**Request Body:**

```json
{
  "type": "DIRECT",
  "memberIds": ["otherUser"],
  "title": null
}
```

**Rules and Field Requirements (DIRECT only):**

- Only DIRECT (one-to-one) conversations are supported.
- `memberIds`: Required. Must contain exactly one identifier: the other user's username OR ObjectId string.
- If a DIRECT conversation between the two users already exists, it will be returned (idempotent behavior).
- `title`: Ignored for DIRECT conversations.

**Response:**

```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": "conv123",
    "type": "DIRECT",
    "title": "Travel Chat",
    "members": [
      {
        "id": "user123",
        "userName": "john",
        "firstName": "John",
        "lastName": "Doe"
      }
    ],
    "createdAt": "2025-01-15T14:30:00Z"
  }
}
```

#### Get Conversation Details

```
GET /api/chat/conversations/{conversationId}
Authorization: Basic <base64-credentials>
```

**Path Parameters:**

- `conversationId`: Valid ObjectId format required

#### Get or Create DIRECT Conversation (convenience)

```
GET /api/chat/conversations/direct/{otherUserId}
Authorization: Basic <base64-credentials>
```

**Path Parameters:**

- `otherUserId`: The other user's username OR ObjectId string.

Returns an existing DIRECT conversation or creates one if none exists.

Note: Adding/removing members is not supported for DIRECT conversations and will return 400 Bad Request if attempted.

### Message Management

#### Send Message

```
POST /api/chat/messages
Authorization: Basic <base64-credentials>
Content-Type: application/json
```

**Request Body:**

```json
{
  "conversationId": "64f2e...abc1",
  "content": "Hello everyone!",
  "kind": "TEXT",
  "replyToMessageId": "msg456",
  "attachments": [
    {
      "mediaId": "media123",
      "caption": "Tokyo sunset photo"
    }
  ]
}
```

**Field Requirements:**

- `conversationId`: Required. Valid ObjectId string
- `content`: Required if kind is TEXT. Cannot be blank
- `kind`: Required. Must be "TEXT", "IMAGE", "FILE", or "SYSTEM"
- `replyToMessageId`: Optional. Valid ObjectId format if provided
- `attachments`: Optional. Array of attachment objects

**Response:**

```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": "msg123",
    "conversationId": "conv123",
    "senderId": "user123",
    "senderName": "John Doe",
    "content": "Hello everyone!",
    "kind": "TEXT",
    "attachments": [
      {
        "id": "media123",
        "filename": "Tokyo sunset photo",
        "url": "media123",
        "contentType": "application/octet-stream",
        "size": 0
      }
    ],
    "createdAt": "2025-01-15T14:30:00Z",
    "readBy": [],
    "readCount": 0
  }
}
```

#### Get Messages

```
GET /api/chat/conversations/{conversationId}/messages?page=0&size=50
Authorization: Basic <base64-credentials>
```

**Query Parameters:**

- `page`: Page number (default: 0)
- `size`: Page size (default: 50, max: 100)

**Path Parameters:**

- `conversationId`: Valid ObjectId format required

#### Edit Message

```
PUT /api/chat/messages/{messageId}
Authorization: Basic <base64-credentials>
Content-Type: application/json
```

**Request Body:**

```json
{
  "content": "Updated message text"
}
```

**Path Parameters:**

- `messageId`: Valid ObjectId format required

**Field Requirements:**

- `content`: Required. Cannot be blank

#### Delete Message

```
DELETE /api/chat/messages/{messageId}
Authorization: Basic <base64-credentials>
```

**Path Parameters:**

- `messageId`: Valid ObjectId format required

#### Mark Messages as Read

```
POST /api/chat/messages/read
Authorization: Basic <base64-credentials>
Content-Type: application/json
```

**Request Body:**

```json
{
  "conversationId": "conv123",
  "lastReadMessageId": "msg456"
}
```

**Field Requirements:**

- `conversationId`: Required. Valid ObjectId format
- `lastReadMessageId`: Required. Valid ObjectId format

#### Get Unread Count

```
GET /api/chat/conversations/{conversationId}/unread-count
Authorization: Basic <base64-credentials>
```

**Path Parameters:**

- `conversationId`: Valid ObjectId format required

**Response:**

```json
{
  "success": true,
  "message": "Unread count retrieved successfully",
  "data": {
    "conversationId": "conv123",
    "unreadCount": 5
  }
}
```

### Error Responses

#### 400 Bad Request

- Invalid ObjectId format for IDs
- Missing required fields
- Invalid field values (e.g., unsupported message kind)

```json
{
  "success": false,
  "message": "Invalid ID format: invalid-id",
  "timestamp": "2025-01-15T14:30:00Z"
}
```

#### 401 Unauthorized

- Missing or invalid Basic Authentication header

```json
{
  "success": false,
  "message": "Authentication required",
  "timestamp": "2025-01-15T14:30:00Z"
}
```

#### 404 Not Found

- Conversation or message not found

```json
{
  "success": false,
  "message": "Conversation not found",
  "timestamp": "2025-01-15T14:30:00Z"
}
```

```

#### Edit Message

```

PUT /api/chat/messages/{messageId}?content=Updated message text

```

#### Delete Message

```

DELETE /api/chat/messages/{messageId}

```

#### Mark Messages as Read

```

POST /api/chat/messages/read

````

**Request Body:**

```json
{
  "conversationId": "conv123",
  "lastReadMessageId": "msg456"
}
````

#### Get Unread Count

```
GET /api/chat/conversations/{conversationId}/unread-count
```

---

## Admin APIs

**Authentication Required:** Basic Auth with ADMIN role

### User Management

#### Get All Users

```
GET /admin/users
```

#### Get Specific User

```
GET /admin/users/{username}
```

#### Delete User

```
DELETE /admin/users/{username}
```

#### Update User Roles

```
PUT /admin/users/{username}/roles
```

**Request Body:**

```json
{
  "roles": ["USER", "ADMIN"]
}
```

#### Set User Status

```
PUT /admin/users/{username}/status
```

**Request Body:**

```json
{
  "active": true
}
```

#### Get System Statistics

```
GET /admin/stats
```

**Response:**

```json
{
  "totalUsers": 150,
  "adminUsers": 3,
  "regularUsers": 147,
  "timestamp": 1640995200000
}
```

#### Create Admin User

```
POST /admin/users
```

**Request Body:**

```json
{
  "userName": "newadmin",
  "password": "adminpass123",
  "firstName": "New",
  "lastName": "Admin",
  "email": "newadmin@example.com"
}
```

#### Reset User Password

```
PUT /admin/users/{username}/password
```

**Request Body:**

```json
{
  "password": "newpassword123"
}
```

#### Promote User to Admin

```
POST /admin/users/{username}/promote
```

**Response:**

```json
{
  "message": "User promoted to admin successfully"
}
```

#### Get Users by Role

```
GET /admin/users/role/{role}
```

**Example:**

```
GET /admin/users/role/ADMIN
```

**Response:** List of users with the specified role

---

## Media Management APIs

**Authentication Required:** Basic Auth

### Media Operations

#### Get Media for Post

```
GET /posts/{postId}/media
```

**Response:**

```json
{
  "success": true,
  "data": [
    {
      "id": "media123",
      "fileName": "vacation-photo.jpg",
      "fileUrl": "/posts/post123/media/media123",
      "fileType": "image/jpeg",
      "fileSize": 2048576,
      "uploaderId": "user123",
      "postId": "post123",
      "uploadedAt": "2025-01-15T14:30:00"
    }
  ]
}
```

#### Upload Media File

```
POST /posts/{postId}/media/upload
```

**Request:** multipart/form-data

- **file**: The media file to upload

**Supported Formats:** JPEG, PNG, GIF, MP4, AVI (max 20MB)

**Response:** `201 Created`

```json
{
  "success": true,
  "message": "Media uploaded successfully",
  "data": {
    "id": "media123",
    "fileName": "vacation-photo.jpg",
    "fileUrl": "/posts/post123/media/media123",
    "fileType": "image/jpeg",
    "fileSize": 2048576,
    "uploaderId": "user123",
    "postId": "post123",
    "uploadedAt": "2025-01-15T14:30:00"
  }
}
```

#### Get Specific Media File

```
GET /posts/{postId}/media/{mediaId}
```

**Response:** Binary file download

#### Delete Media File

```
DELETE /posts/{postId}/media/{mediaId}
```

**Response:**

```json
{
  "success": true,
  "message": "Media deleted successfully"
}
```

_Note: Only the post author can upload or delete media for their posts._

---

## WebSocket Chat

Real-time messaging via WebSocket connection.

### Connection

```
ws://localhost:8080/ws
```

Authentication is handled during the WebSocket handshake using Basic Auth credentials.

### Message Types

#### Send Message

```json
{
  "type": "SEND_MESSAGE",
  "conversationId": "conv123",
  "content": "Hello there!",
  "kind": "TEXT"
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

#### Join Conversation

```json
{
  "type": "JOIN_CONVERSATION",
  "conversationId": "conv123"
}
```

### Subscription Endpoints

- `/topic/conversation/{conversationId}` - Receive messages and events for a conversation
- `/user/queue/notifications` - Receive personal notifications

---

## Examples

### Complete User Registration and First Post

#### 1. Register User

```bash
curl -X POST http://localhost:8080/public/create-user \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "traveler123",
    "password": "mypassword",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com"
  }'
```

#### 2. Create Post (with Basic Auth)

```bash
curl -X POST http://localhost:8080/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dHJhdmVsZXIxMjM6bXlwYXNzd29yZA==" \
  -d '{
    "title": "My First Travel Post",
    "content": "Just visited an amazing place...",
    "location": "Bali, Indonesia",
    "tags": ["travel", "bali", "vacation"],
    "published": true
  }'
```

#### 3. Get User's Posts

```bash
curl http://localhost:8080/posts/user/traveler123
```

### Chat Example (DIRECT)

#### 1. Get or Create DIRECT Conversation

```bash
curl -X GET http://localhost:8080/api/chat/conversations/direct/othertraveler \
  -H "Authorization: Basic dHJhdmVsZXIxMjM6bXlwYXNzd29yZA=="
```

#### 2. Send Message

```bash
curl -X POST http://localhost:8080/api/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dHJhdmVsZXIxMjM6bXlwYXNzd29yZA==" \
  -d '{
    "conversationId": "60b5d9f5e1b2c3d4e5f6g7h8",
    "content": "Hey! Loved your Bali post!",
    "kind": "TEXT"
  }'
```

### Admin Example

#### Promote User to Admin

```bash
curl -X PUT http://localhost:8080/admin/users/traveler123/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW5wYXNz" \
  -d '{
    "roles": ["USER", "ADMIN"]
  }'
```

#### Reset User Password

```bash
curl -X PUT http://localhost:8080/admin/users/traveler123/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW5wYXNz" \
  -d '{
    "password": "newpassword123"
  }'
```

#### Get System Statistics

```bash
curl -X GET http://localhost:8080/admin/stats \
  -H "Authorization: Basic YWRtaW46YWRtaW5wYXNz"
```

### Media Upload Example

#### 1. Upload Media to Post

```bash
curl -X POST http://localhost:8080/posts/60b5d9f5e1b2c3d4e5f6g7h8/media/upload \
  -H "Authorization: Basic dHJhdmVsZXIxMjM6bXlwYXNzd29yZA==" \
  -F "file=@vacation-photo.jpg"
```

#### 2. Get All Media for Post

```bash
curl http://localhost:8080/posts/60b5d9f5e1b2c3d4e5f6g7h8/media
```

#### 3. Download Specific Media File

```bash
curl http://localhost:8080/posts/60b5d9f5e1b2c3d4e5f6g7h8/media/media123 \
  --output downloaded-image.jpg
```

---

## Rate Limits

- Chat messages: 60 per minute per user
- Typing indicators: 2-second cooldown
- File uploads: 20MB max file size, 25MB max request size

---

## File Uploads

File uploads are supported for post media through the Media Management APIs:

```bash
curl -X POST http://localhost:8080/posts/{postId}/media/upload \
  -H "Authorization: Basic <credentials>" \
  -F "file=@image.jpg"
```

Supported formats: JPEG, PNG, GIF, MP4, AVI (up to 20MB each)

See [Media Management APIs](#media-management-apis) for detailed documentation.

---

**For additional details and interactive testing, visit the Swagger UI at:** `http://localhost:8080/swagger-ui.html`
