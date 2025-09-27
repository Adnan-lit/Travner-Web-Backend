# Travner API - Postman Testing Guide

This guide provides detailed instructions for testing the Travner travel blog and social platform API using Postman.

## 📋 Prerequisites

- Postman installed
- Travner backend running at `http://localhost:8080`
- Basic understanding of REST APIs and Basic Authentication

## 🔄 Importing the Collection

1. Open Postman
2. Click "Import" button
3. Import the provided collection file: `travner-social-api-postman.json`
4. The collection will appear in your Postman workspace

## ⚙️ Environment Setup

Create a Postman environment with these variables for easier testing:

| Variable             | Description                | Example Value                     |
| -------------------- | -------------------------- | --------------------------------- |
| `base_url`           | API base URL               | `http://localhost:8080`           |
| `username`           | Test user's username       | `testuser`                        |
| `password`           | Test user's password       | `password123`                     |
| `test_username`      | Username for testing       | `testuser`                        |
| `auth_header`        | Base64 encoded credentials | `dGVzdHVzZXI6cGFzc3dvcmQxMjM=`    |
| `admin_auth`         | Admin user credentials     | `YWRtaW46YWRtaW5wYXNz`            |
| `new_admin_username` | Created admin username     | (set after creating admin)        |
| `new_password_auth`  | Auth after password reset  | (set after password reset)        |
| `reset_token`        | Password reset token       | (set after forgot password)       |
| `post_id`            | ID of created post         | (set after creating post)         |
| `comment_id`         | ID of created comment      | (set after creating comment)      |
| `conversation_id`    | ID of chat conversation    | (set after creating conversation) |
| `message_id`         | ID of chat message         | (set after sending message)       |
| `media_id`           | ID of uploaded media       | (set after uploading media)       |

### Generating Auth Header

```bash
# Generate base64 encoded credentials
echo -n 'testuser:password123' | base64
# Result: dGVzdHVzZXI6cGFzc3dvcmQxMjM=
```

## 🔐 Authentication

**ALL protected endpoints use Basic Authentication:**

```
Authorization: Basic {{auth_header}}
```

The API uses a simple, consistent authentication model - no JWT tokens or complex auth flows.

## 🧪 Testing Workflow

Follow this recommended workflow to test all API features systematically:

### Phase 1: User Registration & Profile

#### 1.1 Register New User

```http
POST {{base_url}}/public/create-user
Content-Type: application/json

{
  "userName": "johndoe",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

#### 1.2 Check Username Availability

```http
GET {{base_url}}/public/check-username/johndoe
```

#### 1.3 Get User Profile (with auth)

```http
GET {{base_url}}/user/profile
Authorization: Basic {{auth_header}}
```

#### 1.4 Update Profile

```http
PUT {{base_url}}/user/profile
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "bio": "Passionate traveler exploring the world",
  "location": "New York, USA"
}
```

#### 1.5 Get Public Profile (no auth)

```http
GET {{base_url}}/user/public/johndoe
```

### Phase 2: Post Management

#### 2.1 Create Post

```http
POST {{base_url}}/posts
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "title": "Amazing Journey Through Tokyo",
  "content": "Tokyo was incredible! The blend of modern and traditional culture...",
  "location": "Tokyo, Japan",
  "tags": ["travel", "japan", "tokyo", "culture", "food"],
  "published": true
}
```

_📝 Copy the returned post ID to your `post_id` environment variable_

#### 2.2 Get All Posts (public)

```http
GET {{base_url}}/posts?page=0&size=10&sortBy=createdAt&direction=desc
```

#### 2.3 Get Specific Post

```http
GET {{base_url}}/posts/{{post_id}}
```

#### 2.4 Search Posts

```http
GET {{base_url}}/posts/search?query=Tokyo&page=0&size=10
```

#### 2.5 Get Posts by Location

```http
GET {{base_url}}/posts/location?location=Tokyo&page=0&size=10
```

#### 2.6 Get Posts by Tags

```http
GET {{base_url}}/posts/tags?tags=travel,japan&page=0&size=10
```

#### 2.7 Update Post

```http
PUT {{base_url}}/posts/{{post_id}}
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "title": "Amazing Journey Through Tokyo - Updated",
  "content": "Tokyo was incredible! The blend of modern and traditional culture, amazing food...",
  "location": "Tokyo, Japan",
  "tags": ["travel", "japan", "tokyo", "culture", "food", "updated"],
  "published": true
}
```

#### 2.8 Vote on Post

```http
POST {{base_url}}/posts/{{post_id}}/upvote
Authorization: Basic {{auth_header}}
```

```http
POST {{base_url}}/posts/{{post_id}}/downvote
Authorization: Basic {{auth_header}}
```

### Phase 3: Comment Management

#### 3.1 Add Comment to Post

```http
POST {{base_url}}/posts/{{post_id}}/comments
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "content": "Great post! I've been to Tokyo too and loved the food scene."
}
```

_📝 Copy the returned comment ID to your `comment_id` environment variable_

#### 3.2 Get Post Comments

```http
GET {{base_url}}/posts/{{post_id}}/comments?page=0&size=10
```

#### 3.3 Update Comment

```http
PUT {{base_url}}/posts/{{post_id}}/comments/{{comment_id}}
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "content": "Great post! I've been to Tokyo too and absolutely loved the food scene and culture."
}
```

#### 3.4 Delete Comment

```http
DELETE {{base_url}}/posts/{{post_id}}/comments/{{comment_id}}
Authorization: Basic {{auth_header}}
```

### Phase 4: Chat System Testing (DIRECT one-to-one)

**⚠️ IMPORTANT: ID/Username Inputs**

- For REST message and conversation endpoints, `conversationId` and `messageId` must be valid MongoDB ObjectId strings (24-character hex).
- For creating or referencing the other user in DIRECT chat, you may use either their username or ObjectId.

#### 4.1 Get or Create Direct Conversation

```http
GET {{base_url}}/api/chat/conversations/direct/{{other_user}}
Authorization: Basic {{auth_header}}
```

_📝 Save the returned conversation id to `{{conversation_id}}`_

#### 4.3 Get User's Conversations

```http
GET {{base_url}}/api/chat/conversations?page=0&size=20
Authorization: Basic {{auth_header}}
```

**✅ Expected Success Response:**

```json
{
  "success": true,
  "data": {
    "content": [...],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 5
  }
}
```

#### 4.4 Get Conversation Details

```http
GET {{base_url}}/api/chat/conversations/{{conversation_id}}
Authorization: Basic {{auth_header}}
```

**❌ Common Errors:**

- 400 Bad Request: Invalid ObjectId format for conversationId
- 404 Not Found: Conversation doesn't exist (now returns 404 instead of 400)

#### 4.5 Send Text Message

```http
POST {{base_url}}/api/chat/messages
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "conversationId": "{{conversation_id}}",
  "kind": "TEXT",
  "content": "Hey! How was your trip to Tokyo?"
}
```

**Field Validation:**

- `conversationId`: Required, valid ObjectId format
- `kind`: Required ("TEXT", "IMAGE", "FILE", "SYSTEM")
- `content`: Required for TEXT messages, cannot be blank

**✅ Expected Success Response:**

```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": "msg123",
    "conversationId": "conv123",
    "senderName": "John Doe",
    "content": "Hey! How was your trip to Tokyo?",
    "kind": "TEXT",
    "attachments": [],
    "createdAt": "2025-01-15T14:30:00Z",
    "readBy": [],
    "readCount": 0
  }
}
```

_📝 Copy the returned message ID to your `message_id` environment variable_

#### 4.6 Send Reply Message

```http
POST {{base_url}}/api/chat/messages
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "conversationId": "{{conversation_id}}",
  "kind": "TEXT",
  "content": "It was amazing! Check out my latest post about it.",
  "replyToMessageId": "{{message_id}}"
}
```

**Field Validation:**

- `replyToMessageId`: Optional, must be valid ObjectId format if provided

#### 4.7 Send Message with Attachments

```http
POST {{base_url}}/api/chat/messages
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "conversationId": "{{conversation_id}}",
  "kind": "IMAGE",
  "content": "Check out this sunset!",
  "attachments": [
    {
      "mediaId": "media123",
      "caption": "Tokyo sunset photo"
    }
  ]
}
```

**Attachment Fields:**

- `mediaId`: Required, attachment identifier
- `caption`: Optional, attachment description

#### 4.8 Get Conversation Messages

```http
GET {{base_url}}/api/chat/conversations/{{conversation_id}}/messages?page=0&size=50
Authorization: Basic {{auth_header}}
```

**Query Parameters:**

- `page`: Page number (default: 0)
- `size`: Page size (default: 50, max: 100)

#### 4.9 Edit Message

```http
PUT {{base_url}}/api/chat/messages/{{message_id}}
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "content": "Hey! How was your amazing trip to Tokyo?"
}
```

**Field Validation:**

- `content`: Required, cannot be blank
- Only the message sender can edit their messages

#### 4.10 Mark Messages as Read

```http
POST {{base_url}}/api/chat/messages/read
Authorization: Basic {{auth_header}}
Content-Type: application/json

{
  "conversationId": "{{conversation_id}}",
  "lastReadMessageId": "{{message_id}}"
}
```

**Field Validation:**

- `conversationId`: Required, valid ObjectId format
- `lastReadMessageId`: Required, valid ObjectId format

#### 4.11 Get Unread Message Count

```http
GET {{base_url}}/api/chat/conversations/{{conversation_id}}/unread-count
Authorization: Basic {{auth_header}}
```

**✅ Expected Success Response:**

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

**🔧 Fixed Issues:**

- Unread count now correctly counts messages per conversation (not globally)
- Better error handling for invalid ObjectIds

#### 4.12 Delete Message

```http
DELETE {{base_url}}/api/chat/messages/{{message_id}}
Authorization: Basic {{auth_header}}
```

**Permissions:**

- Only the message sender or conversation admin can delete messages

Note: Adding/removing members is not supported for DIRECT conversations (will return 400 Bad Request if attempted).

### 🔧 Chat System Improvements Applied

#### Fixed Issues:

1. **ObjectId Validation**: Proper error handling for invalid ID formats
2. **Unread Count Bug**: Now counts messages per conversation, not globally
3. **404 vs 400 Errors**: Missing resources now return 404 Not Found instead of 400 Bad Request
4. **Complete Attachment Data**: Attachment responses now include url, contentType, size fields
5. **Better Error Messages**: More descriptive validation error messages

#### Error Response Examples:

**400 Bad Request - Invalid ObjectId:**

```json
{
  "success": false,
  "message": "Invalid ID format: invalid-id",
  "timestamp": "2025-01-15T14:30:00Z"
}
```

**404 Not Found - Missing Resource:**

```json
{
  "success": false,
  "message": "Conversation not found",
  "timestamp": "2025-01-15T14:30:00Z"
}
```

**400 Bad Request - Validation Error:**

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "content": "Content is required"
  },
  "timestamp": "2025-01-15T14:30:00Z"
}
```

### Phase 5: WebSocket Chat Testing

**Note**: WebSocket testing requires a WebSocket client or browser-based testing. Here's the connection info:

#### 5.1 Connect to WebSocket

- **URL**: `ws://localhost:8080/ws`
- **Authentication**: Include Basic Auth credentials during handshake
- **Protocols**: STOMP over WebSocket

#### 5.2 Subscribe to Conversation Updates

```javascript
// Subscribe to conversation messages
stompClient.subscribe(
  "/topic/conversation/" + conversationId,
  function (message) {
    console.log("New message:", JSON.parse(message.body));
  }
);

// Subscribe to notifications (optional)
stompClient.subscribe("/user/queue/notifications", function (message) {
  console.log("Notification:", JSON.parse(message.body));
});
```

#### 5.3 Send Message via WebSocket

**✅ CORRECTED MESSAGE FORMAT:**

```javascript
stompClient.send(
  "/app/chat.sendMessage",
  {},
  JSON.stringify({
    conversationId: "60f4b2e6c8f1a2c8f4b2e6c9", // Valid ObjectId format
    kind: "TEXT", // Correct field name (not "type")
    content: "Hello from WebSocket!",
    replyToMessageId: "60f4b2e6c8f1a2c8f4b2e6ca", // Optional, must be valid ObjectId
  })
);
```

**Field Requirements:**

- `conversationId`: Required, valid ObjectId format (24-char hex)
- `kind`: Required, must be "TEXT", "IMAGE", "FILE", or "SYSTEM"
- `content`: Required for TEXT messages
- `replyToMessageId`: Optional, valid ObjectId format if provided

#### 5.4 Send Typing Indicator

```javascript
stompClient.send(
  "/app/chat.typing",
  {},
  JSON.stringify({
    conversationId: "60f4b2e6c8f1a2c8f4b2e6c9", // Valid ObjectId format
    isTyping: true,
  })
);
```

#### 5.5 WebSocket Message Response Format

**✅ Expected Message Response:**

```javascript
{
  "id": "60f4b2e6c8f1a2c8f4b2e6cd",
  "conversationId": "60f4b2e6c8f1a2c8f4b2e6c9",
  "senderId": "60f4b2e6c8f1a2c8f4b2e6ce",
  "senderName": "John Doe",
  "content": "Hello from WebSocket!",
  "kind": "TEXT",
  "attachments": [],
  "createdAt": "2025-01-15T14:30:00Z",
  "readBy": [],
  "readCount": 0
}
```

### 🔧 WebSocket Fixes Applied

#### Issues Fixed:

1. **Field Name Correction**: Use `kind` field (not `type`) in WebSocket messages
2. **ObjectId Validation**: All IDs must be valid 24-character hexadecimal format
3. **Complete Response Data**: WebSocket responses now include complete attachment information
4. **Better Error Handling**: Invalid messages are rejected with proper error messages

---

## Phase 6: Admin Management Testing

### 6.1 Get All Users (Admin Only)

**Method**: GET
**URL**: `{{base_url}}/admin/users`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK with list of all users
**Test Script**: Verify user list structure and save first user for testing

### 6.2 Get Specific User by Username (Admin Only)

**Method**: GET
**URL**: `{{base_url}}/admin/users/{{test_username}}`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK with user details

### 6.3 Create Admin User

**Method**: POST
**URL**: `{{base_url}}/admin/users`
**Headers**:

```
Authorization: Basic {{admin_auth}}
Content-Type: application/json
```

**Body**:

```json
{
  "userName": "newadmin",
  "password": "adminpass123",
  "firstName": "New",
  "lastName": "Admin",
  "email": "newadmin@example.com"
}
```

**Expected Response**: 200 OK
**Test Script**: Save new admin username to `{{new_admin_username}}`

### 6.4 Update User Roles (Admin Only)

**Method**: PUT
**URL**: `{{base_url}}/admin/users/{{test_username}}/roles`
**Headers**:

```
Authorization: Basic {{admin_auth}}
Content-Type: application/json
```

**Body**:

```json
{
  "roles": ["USER", "ADMIN"]
}
```

**Expected Response**: 200 OK

### 6.5 Reset User Password (Admin Only)

**Method**: PUT
**URL**: `{{base_url}}/admin/users/{{test_username}}/password`
**Headers**:

```
Authorization: Basic {{admin_auth}}
Content-Type: application/json
```

**Body**:

```json
{
  "password": "newpassword123"
}
```

**Expected Response**: 200 OK

### 6.6 Promote User to Admin

**Method**: POST
**URL**: `{{base_url}}/admin/users/{{test_username}}/promote`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK

### 6.7 Get Users by Role

**Method**: GET
**URL**: `{{base_url}}/admin/users/role/ADMIN`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK with admin users list

### 6.8 Get System Statistics

**Method**: GET
**URL**: `{{base_url}}/admin/stats`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK with system stats

```json
{
  "totalUsers": 150,
  "adminUsers": 3,
  "regularUsers": 147,
  "timestamp": 1640995200000
}
```

### 6.9 Set User Status (Admin Only)

**Method**: PUT
**URL**: `{{base_url}}/admin/users/{{test_username}}/status`
**Headers**:

```
Authorization: Basic {{admin_auth}}
Content-Type: application/json
```

**Body**:

```json
{
  "active": false
}
```

**Expected Response**: 200 OK

### 6.10 Delete User (Admin Only)

**Method**: DELETE
**URL**: `{{base_url}}/admin/users/{{new_admin_username}}`
**Headers**:

```
Authorization: Basic {{admin_auth}}
```

**Expected Response**: 200 OK

---

## Phase 7: Media Upload Testing

### 7.1 Get Media for Post

**Method**: GET
**URL**: `{{base_url}}/posts/{{post_id}}/media`
**Headers**:

```
Authorization: Basic {{auth_header}}
```

**Expected Response**: 200 OK with media list

### 7.2 Upload Media File

**Method**: POST
**URL**: `{{base_url}}/posts/{{post_id}}/media/upload`
**Headers**:

```
Authorization: Basic {{auth_header}}
```

**Body**: form-data

- **Key**: `file`
- **Value**: Select a test image/video file (JPG, PNG, GIF, MP4, AVI - max 20MB)

**Expected Response**: 201 Created
**Test Script**: Save media ID from response to `{{media_id}}`

```javascript
if (pm.response.code === 201) {
  const response = pm.response.json();
  if (response.data && response.data.id) {
    pm.environment.set("media_id", response.data.id);
  }
}
```

### 7.3 Get Specific Media File

**Method**: GET
**URL**: `{{base_url}}/posts/{{post_id}}/media/{{media_id}}`
**Expected Response**: 200 OK with file download

### 7.4 Delete Media File

**Method**: DELETE
**URL**: `{{base_url}}/posts/{{post_id}}/media/{{media_id}}`
**Headers**:

```
Authorization: Basic {{auth_header}}
```

**Expected Response**: 200 OK

---

## Phase 8: Password Reset & Public APIs Testing

### 8.1 Check Username Availability

**Method**: GET
**URL**: `{{base_url}}/public/check-username/testuser999`
**Expected Response**: 200 OK

```json
{
  "message": "Username is available",
  "available": true
}
```

### 8.2 Request Password Reset

**Method**: POST
**URL**: `{{base_url}}/public/forgot-password`
**Headers**:

```
Content-Type: application/json
```

**Body**:

```json
{
  "username": "{{test_username}}"
}
```

**Expected Response**: 200 OK
**Test Script**: Save reset token from response to `{{reset_token}}`

```javascript
if (pm.response.code === 200) {
  const response = pm.response.json();
  if (response.resetToken) {
    pm.environment.set("reset_token", response.resetToken);
  }
}
```

### 8.3 Reset Password with Token

**Method**: POST
**URL**: `{{base_url}}/public/reset-password`
**Headers**:

```
Content-Type: application/json
```

**Body**:

```json
{
  "token": "{{reset_token}}",
  "newPassword": "newpassword456"
}
```

**Expected Response**: 200 OK

### 8.4 Test Login with New Password

**Method**: GET
**URL**: `{{base_url}}/user/profile`
**Headers**:

```
Authorization: Basic {{new_password_auth}}
```

**Expected Response**: 200 OK (confirms password reset worked)

### 8.5 Create First Admin (System Setup)

**Method**: POST
**URL**: `{{base_url}}/public/create-first-admin`
**Headers**:

```
Content-Type: application/json
```

**Body**:

```json
{
  "userName": "sysadmin",
  "password": "sysadminpass123",
  "firstName": "System",
  "lastName": "Admin",
  "email": "sysadmin@example.com"
}
```

**Expected Response**: 200 OK (only works if no admin exists)

---

## Response Validation

For each request, check:

1. Status code (200 for successful GET, 201 for successful POST, etc.)
2. Response body structure matches expected format
3. Data values match what was expected

## Common Issues

- **401 Unauthorized**: Check your username and password
- **403 Forbidden**: You don't have permission for this action
- **404 Not Found**: Check if IDs are correct
- **400 Bad Request**: Check request body format and required fields

### Chat-Specific Issues (Updated)

#### 🔧 **Fixed Issues (After Backend Updates):**

- ✅ **Unread count bug fixed**: Now correctly counts messages per conversation
- ✅ **ObjectId validation**: Better error messages for invalid ID formats
- ✅ **404 vs 400 errors**: Missing resources now return proper 404 Not Found
- ✅ **Complete attachment data**: Attachments now include all required fields

#### ❌ **Common Issues & Solutions:**

**400 Bad Request - Invalid ObjectId Format:**

```
Error: "Invalid ID format: invalid-id"
Solution: Use valid MongoDB ObjectId format (24-character hexadecimal)
Example: "60f4b2e6c8f1a2c8f4b2e6c8"
```

**400 Bad Request - Wrong Field Names:**

```
❌ Wrong: {"conversationId": "...", "type": "TEXT", "content": "..."}
✅ Correct: {"conversationId": "...", "kind": "TEXT", "content": "..."}

❌ Wrong: {"type": "DIRECT", "participantIds": ["..."]}
✅ Correct: {"type": "DIRECT", "memberIds": ["..."]}
```

**400 Bad Request - URL Encoding Issues:**

```
❌ Problem: /api/chat/messages/?content=Hello [UPDATED] ]
✅ Solution: Use POST body instead of query parameters
Or properly encode: /api/chat/messages/?content=Hello%20%5BUPDATED%5D
```

**401 Unauthorized:**

- Ensure Basic Authentication header is included: `Authorization: Basic <base64-credentials>`
- Verify username and password are correct
- Check credentials are base64 encoded properly

**404 Not Found:**

- Conversation or message doesn't exist
- Check ObjectId format is valid
- Verify the resource exists in the system

**403 Forbidden:**

- User doesn't have permission (e.g., not a conversation member)
- Only message sender can edit their messages
- Only conversation admins can remove members

#### 🧪 **Testing ObjectId Validation:**

**Generate Valid Test ObjectIds:**

```javascript
// In browser console or Node.js
const ObjectId = (
  m = Math,
  d = Date,
  h = 16,
  s = (s) => m.floor(s).toString(h)
) => s(d.now() / 1000) + " ".repeat(16).replace(/./g, () => s(m.random() * h));
console.log(ObjectId()); // Example: 60f4b2e6c8f1a2c8f4b2e6c8
```

**Test Invalid ObjectId Handling:**

```http
POST {{base_url}}/api/chat/conversations
{
  "type": "DIRECT",
  "memberIds": ["invalid-id"] // Should return 400 with clear error message
}
```

#### 🔄 **WebSocket Issues:**

**Connection Problems:**

- URL: `ws://localhost:8080/ws`
- Include Basic Auth during handshake
- Verify STOMP protocol support

**Message Format Issues:**

```javascript
❌ Wrong WebSocket message:
{
  "conversationId": "conv123",
  "type": "TEXT",  // Wrong field name
  "content": "Hello"
}

✅ Correct WebSocket message:
{
  "conversationId": "60f4b2e6c8f1a2c8f4b2e6c9",  // Valid ObjectId
  "kind": "TEXT",                                  // Correct field name
  "content": "Hello"
}
```

#### 🎯 **Best Practices:**

1. **Always use valid ObjectId formats** for all ID parameters
2. **Use POST requests with JSON bodies** for complex data (not query parameters)
3. **Include proper Basic Auth headers** in all requests
4. **Check field names match DTOs** (kind vs type, memberIds vs participantIds)
5. **Handle 404 errors** appropriately (resource not found)
6. **Validate request bodies** before sending to avoid 400 errors

### Admin-Specific Issues

- **Admin endpoints returning 403**: Ensure you're using an admin account with ADMIN role
- **"Admin user creation failed"**: Check if admin user already exists or validate input fields
- **"Cannot delete user"**: Some users may have dependencies that prevent deletion
- **"Role update failed"**: Verify role names are correct (USER, ADMIN)

### Media Upload Issues

- **File upload fails**: Check file size (max 20MB) and supported formats (JPEG, PNG, GIF, MP4, AVI)
- **"Post not found" during media upload**: Verify post ID exists and you're the post author
- **Media file not accessible**: Check if media ID is correct and file exists in GridFS
- **"Unauthorized to upload"**: Only post authors can upload media to their posts

### Password Reset Issues

- **Reset token not received**: Check username exists in system (system won't reveal if username exists for security)
- **"Invalid reset token"**: Tokens expire after certain time or may have been used already
- **"Token not found"**: Ensure you're using the exact token from the forgot-password response

### Testing Multiple Users

To test chat functionality properly, you'll need multiple user accounts:

1. Create `testuser1`, `testuser2`, `testuser3` using the registration endpoint
2. Use different Basic Auth headers for each user
3. Test conversations between different users
4. Verify message delivery and read receipts

## Advanced Testing Scenarios

### Posts & Comments

1. **Pagination**: Test with different page and size parameters

   - `GET {{base_url}}/posts?page=0&size=5`

2. **Multiple Tag Filtering**:

   - `GET {{base_url}}/posts/tags?tags=mountains&tags=hiking`

3. **Location Search**:
   - `GET {{base_url}}/posts/location?location=Switzerland`

### Chat System

1. **Message Pagination**: Test message history with different page sizes

   - `GET {{base_url}}/api/chat/conversations/{{conversation_id}}/messages?page=1&size=25`

2. **Multi-User Conversation Flow**:

   - Create users: `testuser1`, `testuser2`, `testuser3`
   - Create group conversation with all users
   - Send messages from different users
   - Test message read receipts from multiple users

3. **Message Types Testing**:

   ```json
   // Text message
   {"conversationId": "...", "kind": "TEXT", "content": "Hello!"}

   // System message (for notifications)
   {"conversationId": "...", "kind": "SYSTEM", "content": "User joined the chat"}
   ```

4. **Conversation Management**:
   - Test adding/removing members from group chats
   - Verify permissions (only admins can remove members)
   - Test conversation archiving/unarchiving

### Admin Management Testing

1. **User Role Management**:

   - Create regular user, promote to admin, demote back to user
   - Test role-based access control
   - Verify admin-only endpoints are properly protected

2. **System Monitoring**:
   - `GET {{base_url}}/admin/stats` - Monitor system health
   - Track user registration trends
   - Monitor admin vs regular user ratios

### Media Management Testing

1. **File Upload Scenarios**:

   - Test different file types (JPEG, PNG, GIF, MP4, AVI)
   - Test file size limits (max 20MB)
   - Test invalid file types (should be rejected)

2. **Media Permission Testing**:
   - Only post authors can upload media to their posts
   - Only post authors can delete their media
   - Test unauthorized access attempts

### Password Reset Flow Testing

1. **Complete Password Reset Flow**:

   ```
   POST /public/forgot-password → GET reset token → POST /public/reset-password → Test login with new password
   ```

2. **Security Testing**:
   - Test invalid tokens
   - Test expired tokens
   - Test token reuse prevention
   - Verify username enumeration protection

## 📚 Additional Resources

- **Main API Documentation**: `docs/api/API_DOCUMENTATION.md`
- **Project Setup**: `README.md`
- **IntelliJ Setup**: `INTELLIJ_SETUP.md`

For any issues or questions, refer to the main documentation or contact the development team.

---

_Last Updated: September 2025_
_API Version: 1.0_
_Travner Backend Testing Guide with Full Chat System Support_
