# Travner API Documentation

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Standard Response Format](#standard-response-format)
4. [Error Handling](#error-handling)
5. [API Endpoints](#api-endpoints)
   - [Public APIs](#public-apis)
   - [User APIs](#user-apis)
   - [Posts APIs](#posts-apis)
   - [Media APIs](#media-apis)
   - [Comments APIs](#comments-apis)
   - [Chat APIs](#chat-apis)
   - [Admin APIs](#admin-apis)
   - [Debug APIs](#debug-apis)

## Overview

Travner is a travel blog and experience sharing platform that allows users to create posts, share media, comment on posts, and interact with travel content. This API provides comprehensive endpoints for managing all aspects of the platform.

**Base URL**: `http://localhost:8080`

## Authentication

The API uses Basic Authentication for protected endpoints. Include an `Authorization` header with each authenticated request:

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

### User Roles

- **USER**: Standard user role, can create and manage own content
- **ADMIN**: Administrative role, can manage all users and content

## Standard Response Format

All enhanced endpoints return a consistent response format:

```json
{
  \"success\": true,
  \"message\": \"Success message\",
  \"data\": { /* Response data */ },
  \"pagination\": { /* Pagination metadata for paginated responses */ }
}
```

## Error Handling

Error responses include appropriate HTTP status codes and descriptive messages:

```json
{
  \"success\": false,
  \"message\": \"Error description\",
  \"data\": null
}
```

---

## API Endpoints

### Public APIs

These endpoints are accessible without authentication.

#### Create User Account

Create a new user account.

**Endpoint**: `POST /public/create-user`

**Request Body**:

```json
{
  \"userName\": \"johndoe\",
  \"password\": \"secretpassword\",
  \"firstName\": \"John\",
  \"lastName\": \"Doe\",
  \"email\": \"john@example.com\"
}
```

**Response**: `201 Created` on success, `400 Bad Request` or `500 Internal Server Error` on failure.

#### Check Username Availability

Check if a username is available for registration.

**Endpoint**: `GET /public/check-username/{username}`

**Response**:

```json
{
  \"message\": \"Username is available\",
  \"available\": true
}
```

#### Forgot Password

Request a password reset token.

**Endpoint**: `POST /public/forgot-password`

**Request Body**:

```json
{
  \"username\": \"johndoe\"
}
```

**Response**:

```json
{
  \"message\": \"If the username exists, a password reset token has been generated.\",
  \"resetToken\": \"abc123xyz\" // Only included for testing
}
```

#### Reset Password

Reset password using a reset token.

**Endpoint**: `POST /public/reset-password`

**Request Body**:

```json
{
  \"token\": \"abc123xyz\",
  \"newPassword\": \"newSecretPassword\"
}
```

**Response**:

```json
{
  \"message\": \"Password reset successfully\"
}
```

#### Create First Admin

Create the first admin user (only works when no admin exists).

**Endpoint**: `POST /public/create-first-admin`

**Request Body**:

```json
{
  \"userName\": \"admin\",
  \"password\": \"adminpassword\",
  \"firstName\": \"Admin\",
  \"lastName\": \"User\",
  \"email\": \"admin@travner.com\"
}
```

**Response**:

```json
{
  \"message\": \"First admin user created successfully\"
}
```

---

### User APIs

These endpoints require user authentication.

**Note**: User endpoints support dual path patterns for compatibility:

- **Legacy paths**: `/user/*`
- **API paths**: `/api/users/*`

Both patterns provide identical functionality and can be used interchangeably.

#### Get Current User Profile

Get the authenticated user's profile information.

**Endpoint**: `GET /user` or `GET /api/users`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: Returns the user object with secure information.

#### Get User Profile

Get the authenticated user's detailed profile information.

**Endpoint**: `GET /user/profile` or `GET /api/users/profile`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: Returns the user object with secure information.

#### Delete Current User Account

Delete the authenticated user's account.

**Endpoint**: `DELETE /user` or `DELETE /api/users`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: `200 OK` on success.

#### Update User Profile (Full Update)

Update the user's complete profile information.

**Endpoint**: `PUT /user/profile` or `PUT /api/users/profile`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"firstName\": \"John\",
  \"lastName\": \"Smith\",
  \"email\": \"john.smith@example.com\"
}
```

**Response**:

```json
{
  \"message\": \"Profile updated successfully\"
}
```

#### Update User Profile (Partial Update)

Partially update user profile information.

**Endpoint**: `PATCH /user/profile` or `PATCH /api/users/profile`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"firstName\": \"John\"
}
```

**Response**:

```json
{
  \"message\": \"Profile updated successfully\"
}
```

#### Change Password

Change the user's password.

**Endpoint**: `PUT /user/password` or `PUT /api/users/password`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"currentPassword\": \"oldPassword\",
  \"newPassword\": \"newPassword123\"
}
```

**Response**:

```json
{
  \"message\": \"Password changed successfully\"
}
```

---

### Posts APIs

#### Get All Published Posts

Retrieve a paginated list of all published posts.

**Endpoint**: `GET /posts`

**Query Parameters**:

- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size
- `sortBy` (default: \"createdAt\"): Field to sort by
- `direction` (default: \"desc\"): Sort direction (\"asc\" or \"desc\")

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Success\",
  \"data\": [
    {
      \"id\": \"68b7a8a25035a56543b201ca\",
      \"title\": \"Amazing Trip to Bali\",
      \"content\": \"I spent two incredible weeks exploring...\",
      \"location\": \"Bali, Indonesia\",
      \"mediaUrls\": [\"/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb\"],
      \"author\": {
        \"id\": \"68b7a8a25035a56543b201c9\",
        \"userName\": \"traveler123\",
        \"firstName\": \"John\",
        \"lastName\": \"Doe\"
      },
      \"createdAt\": \"2023-09-15T10:30:45\",
      \"updatedAt\": \"2023-09-15T10:30:45\",
      \"tags\": [\"beach\", \"vacation\", \"island\"],
      \"upvotes\": 5,
      \"downvotes\": 0,
      \"commentCount\": 3,
      \"published\": true
    }
  ],
  \"pagination\": {
    \"page\": 0,
    \"size\": 10,
    \"totalElements\": 1,
    \"totalPages\": 1,
    \"first\": true,
    \"last\": true
  }
}
```

#### Get Post by ID

Retrieve a specific post by its ID.

**Endpoint**: `GET /posts/{id}`

**Response**: Same format as individual post in the list above.

#### Get Posts by User

Retrieve posts created by a specific user.

**Endpoint**: `GET /posts/user/{username}`

**Query Parameters**:

- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts.

#### Search Posts

Search for posts based on a query term.

**Endpoint**: `GET /posts/search`

**Query Parameters**:

- `query`: Search term to match against post title and content
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts.

#### Get Posts by Location

Retrieve posts for a specific location.

**Endpoint**: `GET /posts/location`

**Query Parameters**:

- `location`: Location name to match (full or partial)
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts.

#### Get Posts by Tags

Retrieve posts that contain specific tags.

**Endpoint**: `GET /posts/tags`

**Query Parameters**:

- `tags`: List of tags to filter by (can be specified multiple times)
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts.

#### Create Post

Create a new post. Requires authentication.

**Endpoint**: `POST /posts`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"title\": \"My Adventure in Thailand\",
  \"content\": \"This was an amazing trip...\",
  \"location\": \"Bangkok, Thailand\",
  \"tags\": [\"adventure\", \"thailand\", \"travel\"],
  \"published\": true
}
```

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Post created successfully\",
  \"data\": {
    \"id\": \"68b7a8a25035a56543b201cd\",
    \"title\": \"My Adventure in Thailand\",
    \"content\": \"This was an amazing trip...\",
    \"location\": \"Bangkok, Thailand\",
    \"mediaUrls\": [],
    \"author\": {
      \"id\": \"68b7a8a25035a56543b201c9\",
      \"userName\": \"traveler123\",
      \"firstName\": \"John\",
      \"lastName\": \"Doe\"
    },
    \"createdAt\": \"2023-09-15T10:30:45\",
    \"updatedAt\": \"2023-09-15T10:30:45\",
    \"tags\": [\"adventure\", \"thailand\", \"travel\"],
    \"upvotes\": 0,
    \"downvotes\": 0,
    \"commentCount\": 0,
    \"published\": true
  }
}
```

#### Update Post

Update an existing post. Requires authentication and ownership.

**Endpoint**: `PUT /posts/{id}`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**: Same as Create Post

**Response**: Same as Create Post but with \"Post updated successfully\" message.

#### Delete Post

Delete a post. Requires authentication and ownership.

**Endpoint**: `DELETE /posts/{id}`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Post deleted successfully\",
  \"data\": null
}
```

#### Upvote Post

Upvote a post. Requires authentication.

**Endpoint**: `POST /posts/{id}/upvote`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Post upvoted successfully\",
  \"data\": { /* Updated post data */ }
}
```

#### Downvote Post

Downvote a post. Requires authentication.

**Endpoint**: `POST /posts/{id}/downvote`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: Same as upvote but with \"Post downvoted successfully\" message.

---

### Media APIs

#### Get All Media for a Post

Retrieve all media associated with a specific post.

**Endpoint**: `GET /posts/{postId}/media`

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Success\",
  \"data\": [
    {
      \"id\": \"68b7a8a25035a56543b201cb\",
      \"fileName\": \"beach.jpg\",
      \"fileUrl\": \"/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb\",
      \"fileType\": \"image/jpeg\",
      \"fileSize\": 2048576,
      \"uploaderId\": \"68b7a8a25035a56543b201c9\",
      \"postId\": \"68b7a8a25035a56543b201ca\",
      \"uploadedAt\": \"2023-09-15T10:35:12\"
    }
  ]
}
```

#### Get Media File

Retrieve the actual media file. No authentication required.

**Endpoint**: `GET /posts/{postId}/media/{mediaId}`

**Response**: The actual media file with appropriate content type header.

#### Upload Media

Upload media for a post. Requires authentication and post ownership.

**Endpoint**: `POST /posts/{postId}/media/upload`

**Headers**:

```
Content-Type: multipart/form-data
Authorization: Basic <credentials>
```

**Form Data**:

- `file`: The file to upload

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Media uploaded successfully\",
  \"data\": {
    \"id\": \"68b7a8a25035a56543b201cb\",
    \"fileName\": \"beach.jpg\",
    \"fileUrl\": \"/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb\",
    \"fileType\": \"image/jpeg\",
    \"fileSize\": 2048576,
    \"uploaderId\": \"68b7a8a25035a56543b201c9\",
    \"postId\": \"68b7a8a25035a56543b201ca\",
    \"uploadedAt\": \"2023-09-15T10:35:12\"
  }
}
```

#### Delete Media

Delete a media file. Requires authentication and ownership.

**Endpoint**: `DELETE /posts/{postId}/media/{mediaId}`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**:

```json
{
  \"success\": true,
  \"message\": \"Media deleted successfully\",
  \"data\": null
}
```

---

### Comments APIs

#### Get Comments for a Post

Retrieve comments for a specific post with pagination.

**Endpoint**: `GET /posts/{postId}/comments`

**Query Parameters**:

- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**:

```json
{
  \"content\": [
    {
      \"id\": \"68b7a8a25035a56543b201cf\",
      \"content\": \"Great post! I loved visiting Bali too.\",
      \"author\": {
        \"id\": \"68b7a8a25035a56543b201d0\",
        \"userName\": \"commenter1\",
        \"firstName\": \"Jane\",
        \"lastName\": \"Smith\"
      },
      \"createdAt\": \"2023-09-15T11:00:00\",
      \"upvotes\": 2,
      \"downvotes\": 0,
      \"replyCount\": 1,
      \"parentCommentId\": null
    }
  ],
  \"pageable\": { /* pagination info */ },
  \"totalElements\": 5,
  \"totalPages\": 1
}
```

#### Get Comment by ID

Retrieve a specific comment by its ID.

**Endpoint**: `GET /posts/{postId}/comments/{id}`

**Response**: Individual comment object as shown above.

#### Create Comment

Create a new comment on a post. Requires authentication.

**Endpoint**: `POST /posts/{postId}/comments`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"content\": \"This is a great post! Thanks for sharing.\",
  \"parentCommentId\": null  // Optional, for reply to another comment
}
```

**Response**: `201 Created` with the created comment object.

#### Update Comment

Update an existing comment. Requires authentication and ownership.

**Endpoint**: `PUT /posts/{postId}/comments/{id}`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request Body**:

```json
{
  \"content\": \"Updated comment content\"
}
```

**Response**: `200 OK` with the updated comment object.

#### Delete Comment

Delete a comment. Requires authentication and ownership.

**Endpoint**: `DELETE /posts/{postId}/comments/{id}`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: `204 No Content` on success.

#### Upvote Comment

Upvote a comment. Requires authentication.

**Endpoint**: `POST /posts/{postId}/comments/{id}/upvote`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: `200 OK` with the updated comment object.

#### Downvote Comment

Downvote a comment. Requires authentication.

**Endpoint**: `POST /posts/{postId}/comments/{id}/downvote`

**Headers**:

```
Authorization: Basic <credentials>
```

**Response**: `200 OK` with the updated comment object.

---

### Chat APIs

The Travner platform includes a comprehensive real-time chat system with both REST APIs for traditional operations and WebSocket support for real-time messaging.

#### Chat System Overview

- **REST APIs**: Use Basic Authentication for conversation and message management
- **WebSocket**: Uses JWT authentication for real-time messaging
- **Conversation Types**: Direct (1-on-1) and Group conversations
- **Features**: Real-time messaging, typing indicators, read receipts, file attachments

#### Important Notes

- **Authentication Split**: REST endpoints use Basic Auth, WebSocket connections require JWT tokens
- **Real-time Features**: WebSocket connection at `/ws` endpoint with SockJS fallback
- **Message Types**: TEXT, IMAGE, FILE, and SYSTEM messages supported
- **Permissions**: Conversation admins can manage members and settings

#### Quick Reference

For detailed chat API documentation including all endpoints, request/response formats, and WebSocket event handling, please refer to:

**[📖 Complete Chat API Documentation](./CHAT_API_DOCUMENTATION.md)**

#### Key Endpoints Summary

**Authentication**:

- `POST /api/auth/token` - Generate JWT token for WebSocket authentication
- `POST /api/auth/validate` - Validate JWT token

**Conversations**:

- `POST /api/chat/conversations` - Create conversation
- `GET /api/chat/conversations` - Get user's conversations
- `GET /api/chat/conversations/{id}` - Get conversation details
- `POST /api/chat/conversations/{id}/members` - Add members
- `DELETE /api/chat/conversations/{id}/members/{userId}` - Remove member

**Messages**:

- `POST /api/chat/messages` - Send message
- `GET /api/chat/conversations/{id}/messages` - Get messages
- `PUT /api/chat/messages/{id}` - Edit message
- `DELETE /api/chat/messages/{id}` - Delete message
- `POST /api/chat/messages/read` - Mark messages as read

**WebSocket**:

- Connection: `ws://localhost:8080/ws` (with JWT auth)
- Real-time messaging, typing indicators, presence updates
- Event-driven architecture with full chat functionality

---

### Admin APIs

These endpoints require admin authentication.

#### Get All Users

Retrieve all users in the system.

**Endpoint**: `GET /admin/users`

**Headers**:

```
Authorization: Basic <admin-credentials>
```

**Response**: Array of user objects.

#### Get User by Username

Retrieve a specific user by username.

**Endpoint**: `GET /admin/users/{username}`

**Headers**:

```
Authorization: Basic <admin-credentials>
```

**Response**: User object with secure information.

#### Delete User

Delete a user account (admin cannot delete own account).

**Endpoint**: `DELETE /admin/users/{username}`

**Headers**:

```
Authorization: Basic <admin-credentials>
```

**Response**:

```json
{
  \"message\": \"User deleted successfully\"
}
```

#### Update User Roles

Update a user's roles.

**Endpoint**: `PUT /admin/users/{username}/roles`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <admin-credentials>
```

**Request Body**:

```json
{
  \"roles\": [\"USER\", \"ADMIN\"]
}
```

**Response**:

```json
{
  \"message\": \"User roles updated successfully\"
}
```

---

### Debug APIs

These endpoints are for development and testing purposes.

#### Get Post Count

Get statistics about posts in the system.

**Endpoint**: `GET /debug/post-count`

**Response**:

```json
{
  \"totalPosts\": 25,
  \"publishedPosts\": 20
}
```

#### Create Test Post

Create a test post for debugging purposes.

**Endpoint**: `POST /debug/create-test-post`

**Response**:

```json
{
  \"success\": true,
  \"postId\": \"68b7a8a25035a56543b201cd\",
  \"published\": true
}
```

#### Fix Posts

Force all posts to be published (for testing).

**Endpoint**: `POST /debug/fix-posts`

**Response**:

```json
{
  \"totalPosts\": 25,
  \"fixedPosts\": 5
}
```

---

## HTTP Status Codes

The API uses standard HTTP status codes:

- `200 OK`: Request successful
- `201 Created`: Resource created successfully
- `204 No Content`: Request successful, no response body
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Authentication required or invalid
- `403 Forbidden`: Access denied (insufficient permissions)
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists
- `500 Internal Server Error`: Server error

## Rate Limiting

Currently, no rate limiting is implemented, but it's recommended for production use.

## CORS

Cross-Origin Resource Sharing (CORS) is enabled for all origins during development. For production, configure specific allowed origins.

## Data Models

### User Object

```json
{
  \"id\": \"68b7a8a25035a56543b201c9\",
  \"userName\": \"traveler123\",
  \"firstName\": \"John\",
  \"lastName\": \"Doe\",
  \"email\": \"john@example.com\",
  \"roles\": [\"USER\"],
  \"createdAt\": \"2023-09-15T08:00:00\",
  \"updatedAt\": \"2023-09-15T08:00:00\"
}
```

### Post Object

```json
{
  \"id\": \"68b7a8a25035a56543b201ca\",
  \"title\": \"Amazing Trip to Bali\",
  \"content\": \"Content of the post...\",
  \"location\": \"Bali, Indonesia\",
  \"mediaUrls\": [\"/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb\"],
  \"author\": { /* User summary object */ },
  \"createdAt\": \"2023-09-15T10:30:45\",
  \"updatedAt\": \"2023-09-15T10:30:45\",
  \"tags\": [\"beach\", \"vacation\", \"island\"],
  \"upvotes\": 5,
  \"downvotes\": 0,
  \"commentCount\": 3,
  \"published\": true
}
```

### Comment Object

```json
{
  \"id\": \"68b7a8a25035a56543b201cf\",
  \"content\": \"Great post content!\",
  \"author\": { /* User summary object */ },
  \"createdAt\": \"2023-09-15T11:00:00\",
  \"upvotes\": 2,
  \"downvotes\": 0,
  \"replyCount\": 1,
  \"parentCommentId\": null
}
```

### Media Object

```json
{
  \"id\": \"68b7a8a25035a56543b201cb\",
  \"fileName\": \"beach.jpg\",
  \"fileUrl\": \"/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb\",
  \"fileType\": \"image/jpeg\",
  \"fileSize\": 2048576,
  \"uploaderId\": \"68b7a8a25035a56543b201c9\",
  \"postId\": \"68b7a8a25035a56543b201ca\",
  \"uploadedAt\": \"2023-09-15T10:35:12\"
}
```
