# 🚀 Travner Chat API Documentation

## Base URL

- **Development**: `http://localhost:8080`
- **Production**: `https://your-railway-app.railway.app`

## Authentication

### REST API Endpoints

REST endpoints use Basic Authentication via the `Authorization` header:

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

### WebSocket Authentication

WebSocket connections use JWT authentication for real-time features:

```
Authorization: Bearer <jwt-token>
```

#### Getting JWT Token for WebSocket

Before connecting to WebSocket, you need to obtain a JWT token:

**1. Generate JWT Token:**

```http
POST /api/auth/token
Authorization: Basic <base64-encoded-credentials>
Content-Type: application/json
```

**Response:**

```json
{
  "success": true,
  "message": "JWT token generated successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "issuedAt": "2025-09-22T10:30:00Z",
    "username": "john_doe"
  }
}
```

**2. Use Token for WebSocket Connection:**

```javascript
// Frontend example
const token = response.data.accessToken;
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: `Bearer ${token}` },
  function(frame) {
    console.log('Connected: ' + frame);
    // Subscribe to channels and send messages
  }
);
```

---

## � Authentication API Endpoints

### Generate JWT Token

```http
POST /api/auth/token
Authorization: Basic <base64-encoded-credentials>
```

**Response:**

```json
{
  "success": true,
  "message": "JWT token generated successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "issuedAt": "2025-09-22T10:30:00Z",
    "username": "john_doe"
  }
}
```

### Validate JWT Token

```http
POST /api/auth/validate?token=<jwt-token>
```

**Response:**

```json
{
  "success": true,
  "message": "Token is valid",
  "data": "john_doe"
}
```

---

## �💬 Chat REST API Endpoints

### Conversations

#### Create Conversation

```http
POST /api/chat/conversations
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "type": "DIRECT|GROUP",
  "title": "Group Chat Name", // Required for GROUP, optional for DIRECT
  "memberIds": ["userId1", "userId2"] // Optional - other members to add
}
```

**Response:**

```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": "conversationId",
    "type": "GROUP",
    "title": "Group Chat Name",
    "members": [
      {
        "userId": "userId1",
        "userName": "John Doe",
        "firstName": "John",
        "lastName": "Doe",
        "role": "ADMIN",
        "lastReadAt": "2025-09-17T10:30:00Z",
        "muted": false,
        "joinedAt": "2025-09-17T10:30:00Z"
      }
    ],
    "ownerId": "currentUserId",
    "adminIds": ["currentUserId"],
    "createdAt": "2025-09-17T10:30:00Z",
    "lastMessageAt": "2025-09-17T10:30:00Z",
    "isArchived": false,
    "unreadCount": 0
  }
}
```

#### Get User Conversations

```http
GET /api/chat/conversations?page=0&size=20&sort=lastMessageAt,desc
Authorization: Bearer <jwt-token>
```

**Response:**

```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": {
    "content": [
      /* Array of conversations */
    ],
    "pageable": {
      /* pagination info */
    },
    "totalElements": 25,
    "totalPages": 2,
    "size": 20,
    "number": 0
  },
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 25,
    "totalPages": 2,
    "first": true,
    "last": false
  }
}
```

#### Get Conversation Details

```http
GET /api/chat/conversations/{conversationId}
Authorization: Bearer <jwt-token>
```

#### Add Members to Conversation

```http
POST /api/chat/conversations/{conversationId}/members
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "userIds": ["userId1", "userId2"]
}
```

#### Remove Member from Conversation

```http
DELETE /api/chat/conversations/{conversationId}/members/{userId}
Authorization: Bearer <jwt-token>
```

---

### Messages

#### Send Message

```http
POST /api/chat/messages
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "conversationId": "conversationId",
  "kind": "TEXT|IMAGE|FILE|SYSTEM",
  "content": "Hello, world!",
  "attachments": [
    {
      "mediaId": "mediaId",
      "caption": "Image caption"
    }
  ],
  "replyToMessageId": "messageId" // Optional
}
```

**Response:**

```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": "messageId",
    "conversationId": "conversationId",
    "senderId": "userId",
    "senderName": "John Doe",
    "kind": "TEXT",
    "content": "Hello, world!",
    "attachments": [
      {
        "mediaId": "mediaId",
        "fileName": "image.jpg",
        "contentType": "image/jpeg",
        "fileSize": 1024000,
        "downloadUrl": "/api/media/mediaId",
        "caption": "Image caption"
      }
    ],
    "replyToMessageId": null,
    "replyToMessage": null,
    "sentAt": "2025-09-17T10:30:00Z",
    "editedAt": null,
    "isEdited": false,
    "readByUserIds": ["userId"],
    "readCount": 1
  }
}
```

#### Get Messages for Conversation

```http
GET /api/chat/conversations/{conversationId}/messages?page=0&size=50&sort=createdAt,desc
Authorization: Bearer <jwt-token>
```

#### Edit Message

```http
PUT /api/chat/messages/{messageId}?content=Updated message content
Authorization: Bearer <jwt-token>
```

#### Delete Message

```http
DELETE /api/chat/messages/{messageId}
Authorization: Bearer <jwt-token>
```

#### Mark Messages as Read

```http
POST /api/chat/messages/read
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "conversationId": "conversationId",
  "lastReadMessageId": "messageId"
}
```

#### Get Unread Message Count

```http
GET /api/chat/conversations/{conversationId}/unread-count
Authorization: Bearer <jwt-token>
```

**Response:**

```json
{
  "success": true,
  "message": "Unread count retrieved successfully",
  "data": 5
}
```

---

## 🔌 WebSocket Real-Time API

### Connection

```javascript
// Connect to WebSocket
const socket = new SockJS("http://localhost:8080/ws");
const stompClient = Stomp.over(socket);

// Connect with JWT authentication
stompClient.connect(
  {
    Authorization: "Bearer " + jwtToken,
  },
  onConnected,
  onError
);
```

### Subscribe to Events

#### Subscribe to Conversation Messages

```javascript
stompClient.subscribe(
  "/topic/conversation/" + conversationId,
  function (message) {
    const event = JSON.parse(message.body);
    handleChatEvent(event);
  }
);
```

#### Subscribe to User-Specific Events

```javascript
stompClient.subscribe("/user/queue/notifications", function (message) {
  const event = JSON.parse(message.body);
  handleUserEvent(event);
});

stompClient.subscribe("/user/queue/presence", function (message) {
  const event = JSON.parse(message.body);
  handlePresenceEvent(event);
});
```

### Send Real-Time Events

#### Send Message (WebSocket)

```javascript
stompClient.send(
  "/app/chat.sendMessage",
  {},
  JSON.stringify({
    conversationId: "conversationId",
    kind: "TEXT",
    content: "Hello via WebSocket!",
    attachments: [],
    replyToMessageId: null,
  })
);
```

#### Send Typing Indicator

```javascript
// User started typing
stompClient.send(
  "/app/chat.typing",
  {},
  JSON.stringify({
    conversationId: "conversationId",
    isTyping: true,
  })
);

// User stopped typing
stompClient.send(
  "/app/chat.typing",
  {},
  JSON.stringify({
    conversationId: "conversationId",
    isTyping: false,
  })
);
```

#### Update Presence

```javascript
stompClient.send(
  "/app/chat.presence",
  {},
  JSON.stringify({
    type: "USER_ONLINE", // or 'USER_OFFLINE'
  })
);
```

#### Send Read Receipt

```javascript
stompClient.send(
  "/app/chat.messageRead",
  {},
  JSON.stringify({
    conversationId: "conversationId",
    data: { messageId: "lastReadMessageId" },
  })
);
```

---

## 📡 WebSocket Event Types

### Incoming Events (from server)

```typescript
interface ChatEventDTO {
  type:
    | "MESSAGE_SENT"
    | "MESSAGE_EDITED"
    | "MESSAGE_DELETED"
    | "USER_TYPING"
    | "USER_STOPPED_TYPING"
    | "USER_JOINED_CONVERSATION"
    | "USER_LEFT_CONVERSATION"
    | "CONVERSATION_CREATED"
    | "CONVERSATION_UPDATED"
    | "USER_ONLINE"
    | "USER_OFFLINE"
    | "MESSAGE_READ";
  conversationId: string;
  userId: string;
  userName: string;
  data: any; // MessageResponse, ConversationResponse, or event-specific data
  timestamp: string; // ISO 8601 timestamp
}
```

### Event Handling Examples

#### Message Events

```javascript
function handleChatEvent(event) {
  switch (event.type) {
    case "MESSAGE_SENT":
      addMessageToUI(event.data);
      break;

    case "MESSAGE_EDITED":
      updateMessageInUI(event.data);
      break;

    case "MESSAGE_DELETED":
      removeMessageFromUI(event.data.id);
      break;

    case "MESSAGE_READ":
      updateReadReceipts(event.conversationId, event.userId);
      break;
  }
}
```

#### Typing Indicators

```javascript
function handleTypingEvent(event) {
  if (event.type === "USER_TYPING") {
    showTypingIndicator(event.userId, event.userName);
  } else if (event.type === "USER_STOPPED_TYPING") {
    hideTypingIndicator(event.userId);
  }
}
```

#### Presence Updates

```javascript
function handlePresenceEvent(event) {
  if (event.type === "USER_ONLINE") {
    updateUserStatus(event.userId, "online");
  } else if (event.type === "USER_OFFLINE") {
    updateUserStatus(event.userId, "offline");
  }
}
```

---

## 🔐 Authentication & Security

### JWT Token Format

```json
{
  "sub": "userId",
  "iat": 1695814200,
  "exp": 1695900600,
  "authorities": ["ROLE_USER"]
}
```

### WebSocket Authentication

The WebSocket connection requires the JWT token in the connection headers:

```javascript
const headers = {
  Authorization: "Bearer " + localStorage.getItem("jwt_token"),
};
stompClient.connect(headers, onConnected, onError);
```

### Error Handling

```javascript
function onError(error) {
  console.error("WebSocket connection error:", error);
  // Handle reconnection logic
  setTimeout(() => {
    connectToWebSocket();
  }, 5000);
}
```

---

## 🚀 Example Frontend Integration

### Complete Angular WebSocket Service

```typescript
import { Injectable } from "@angular/core";
import { Client, Frame } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { BehaviorSubject, Observable } from "rxjs";

@Injectable({ providedIn: "root" })
export class WebSocketService {
  private stompClient: Client;
  private connected$ = new BehaviorSubject<boolean>(false);

  constructor() {
    this.initializeWebSocketConnection();
  }

  private initializeWebSocketConnection(): void {
    this.stompClient = new Client({
      brokerURL: null, // Use SockJS
      connectHeaders: {
        Authorization: "Bearer " + this.getToken(),
      },
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Use SockJS for better compatibility
    this.stompClient.webSocketFactory = () => {
      return new SockJS("http://localhost:8080/ws");
    };

    this.stompClient.onConnect = (frame: Frame) => {
      console.log("Connected: " + frame);
      this.connected$.next(true);
    };

    this.stompClient.onStompError = (frame: Frame) => {
      console.error("Broker reported error: " + frame.headers["message"]);
      console.error("Additional details: " + frame.body);
      this.connected$.next(false);
    };
  }

  connect(): void {
    this.stompClient.activate();
  }

  disconnect(): void {
    this.stompClient.deactivate();
    this.connected$.next(false);
  }

  subscribeToConversation(conversationId: string): Observable<any> {
    return new Observable((observer) => {
      const subscription = this.stompClient.subscribe(
        `/topic/conversation/${conversationId}`,
        (message) => observer.next(JSON.parse(message.body))
      );

      return () => subscription.unsubscribe();
    });
  }

  sendMessage(message: any): void {
    this.stompClient.publish({
      destination: "/app/chat.sendMessage",
      body: JSON.stringify(message),
    });
  }

  sendTypingIndicator(conversationId: string, isTyping: boolean): void {
    this.stompClient.publish({
      destination: "/app/chat.typing",
      body: JSON.stringify({ conversationId, isTyping }),
    });
  }

  private getToken(): string {
    return localStorage.getItem("jwt_token") || "";
  }
}
```

This comprehensive API documentation provides all the REST endpoints and WebSocket integration details needed for the frontend development! 🎉
