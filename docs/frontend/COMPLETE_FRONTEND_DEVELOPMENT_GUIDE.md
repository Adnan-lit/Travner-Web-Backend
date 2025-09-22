# 🚀 Travner Frontend Development Guide

## Complete Step-by-Step Instructions for AI Agents

---

## 📋 Project Overview

**Travner** is a modern travel social platform with real-time chat capabilities. You will build a responsive Angular frontend that integrates with the provided Spring Boot backend.

### Key Features:

- 🔐 **User Authentication** (Basic Auth for REST APIs)
- 📝 **Travel Blog Platform** (Posts, comments, media uploads)
- 💬 **Real-time Chat System** (WebSocket with JWT authentication)
- 👤 **User Profile Management**
- 📱 **Mobile-first Responsive Design**

---

## 🏗️ Backend Integration Details

### Authentication Architecture:

- **REST APIs**: Use Basic Authentication (username:password encoded in Base64)
- **WebSocket**: Uses JWT tokens for real-time chat connections
- **Base API URL**: `http://localhost:8080/api`
- **WebSocket URL**: `ws://localhost:8080/ws`

### Available API Endpoints:

```
🔐 Authentication & User Management
├── POST   /api/users/register         - User registration
├── GET    /api/users/profile          - Get user profile (Basic Auth)
├── PUT    /api/users/profile          - Update profile (Basic Auth)

📝 Travel Posts Management
├── POST   /api/posts                  - Create post (Basic Auth)
├── GET    /api/posts                  - Get posts (paginated, Basic Auth)
├── GET    /api/posts/{id}             - Get single post (Basic Auth)
├── PUT    /api/posts/{id}             - Update post (Basic Auth)
├── DELETE /api/posts/{id}             - Delete post (Basic Auth)
├── POST   /api/posts/{id}/comments    - Add comment (Basic Auth)
├── GET    /api/posts/{id}/comments    - Get comments (Basic Auth)

📁 Media Management
├── POST   /api/media                  - Upload files (Basic Auth)
├── GET    /api/media/{id}             - Download files (Basic Auth)

💬 Chat System
├── POST   /api/chat/conversations            - Create conversation (Basic Auth)
├── GET    /api/chat/conversations            - Get conversations (Basic Auth)
├── GET    /api/chat/conversations/{id}       - Get conversation details (Basic Auth)
├── POST   /api/chat/conversations/{id}/members - Add members (Basic Auth)
├── DELETE /api/chat/conversations/{id}/members/{userId} - Remove member (Basic Auth)
├── POST   /api/chat/messages                 - Send message (Basic Auth)
├── GET    /api/chat/conversations/{id}/messages - Get messages (Basic Auth)
├── PUT    /api/chat/messages/{id}            - Edit message (Basic Auth)
├── DELETE /api/chat/messages/{id}            - Delete message (Basic Auth)
├── POST   /api/chat/messages/read            - Mark as read (Basic Auth)

🌐 WebSocket Chat Endpoints (JWT Authentication)
├── CONNECT /ws                        - WebSocket connection
├── SEND    /app/chat.sendMessage      - Send real-time message
├── SEND    /app/chat.typing           - Typing indicator
├── SUBSCRIBE /topic/conversations/{id} - Subscribe to conversation
├── SUBSCRIBE /user/queue/messages     - Personal message queue

📊 Public Data
├── GET    /api/public/stats           - Platform statistics (No auth)
```

---

## 🎯 Phase 1: Project Setup & Foundation

**Time Estimate: 1-2 days**

### Step 1.1: Initialize Angular Project

```bash
# Create Angular project with routing and SCSS
ng new travner-frontend --routing --style=scss --skip-git
cd travner-frontend

# Install Material Design and dependencies
npm install @angular/material @angular/cdk @angular/animations
npm install @angular/flex-layout
npm install ngx-toastr

# Add Material Design
ng add @angular/material --theme=indigo-pink --typography=true --animations=true
```

### Step 1.2: Project Structure

```bash
# Generate core modules
ng generate module core
ng generate module shared
ng generate module features/auth --routing
ng generate module features/posts --routing
ng generate module features/chat --routing
ng generate module features/profile --routing

# Generate core services
ng generate service core/services/auth
ng generate service core/services/api
ng generate service core/services/notification

# Generate guards and interceptors
ng generate guard core/guards/auth
ng generate interceptor core/interceptors/auth
ng generate interceptor core/interceptors/error

# Generate auth components
ng generate component features/auth/login
ng generate component features/auth/register
ng generate component shared/components/navbar
ng generate component shared/components/loading-spinner
```

### Step 1.3: Environment Configuration

**src/environments/environment.ts**

```typescript
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080/api",
  wsUrl: "ws://localhost:8080/ws",
  fileUploadMaxSize: 20 * 1024 * 1024, // 20MB
  credentialsKey: "travner_credentials", // For Basic Auth
  jwtTokenKey: "travner_jwt_token", // For WebSocket only
};
```

### Step 1.4: Core Models

**src/app/core/models/user.model.ts**

```typescript
export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  bio?: string;
  location?: string;
  joinedAt: Date;
  isActive: boolean;
}

export interface UserCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  token?: string; // Only for WebSocket JWT
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
```

### Step 1.5: Authentication Service

**src/app/core/services/auth.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { BehaviorSubject, Observable, of } from "rxjs";
import { map, tap, catchError } from "rxjs/operators";
import { environment } from "../../../environments/environment";
import { User, UserCredentials, RegisterRequest } from "../models/user.model";

@Injectable({
  providedIn: "root",
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadStoredCredentials();
  }

  // Store credentials for Basic Auth
  private storeCredentials(username: string, password: string): void {
    const credentials = btoa(`${username}:${password}`);
    localStorage.setItem(environment.credentialsKey, credentials);
  }

  // Get stored credentials
  getStoredCredentials(): string | null {
    return localStorage.getItem(environment.credentialsKey);
  }

  // Create Basic Auth header
  private createAuthHeader(): HttpHeaders {
    const credentials = this.getStoredCredentials();
    if (credentials) {
      return new HttpHeaders({
        Authorization: `Basic ${credentials}`,
      });
    }
    return new HttpHeaders();
  }

  // Register new user
  register(registerData: RegisterRequest): Observable<User> {
    return this.http
      .post<User>(`${environment.apiUrl}/users/register`, registerData)
      .pipe(
        tap((user) => {
          this.currentUserSubject.next(user);
          // Store credentials after successful registration
          this.storeCredentials(registerData.username, registerData.password);
        })
      );
  }

  // Login with Basic Auth
  login(credentials: UserCredentials): Observable<User> {
    const headers = new HttpHeaders({
      Authorization: `Basic ${btoa(
        credentials.username + ":" + credentials.password
      )}`,
    });

    return this.http
      .get<User>(`${environment.apiUrl}/users/profile`, { headers })
      .pipe(
        tap((user) => {
          this.currentUserSubject.next(user);
          this.storeCredentials(credentials.username, credentials.password);
        })
      );
  }

  // Logout
  logout(): void {
    localStorage.removeItem(environment.credentialsKey);
    localStorage.removeItem(environment.jwtTokenKey);
    this.currentUserSubject.next(null);
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return !!this.getStoredCredentials() && !!this.currentUserSubject.value;
  }

  // Load stored credentials and validate
  private loadStoredCredentials(): void {
    const credentials = this.getStoredCredentials();
    if (credentials) {
      // Validate credentials by fetching profile
      const headers = new HttpHeaders({
        Authorization: `Basic ${credentials}`,
      });

      this.http
        .get<User>(`${environment.apiUrl}/users/profile`, { headers })
        .pipe(
          catchError(() => {
            this.logout();
            return of(null);
          })
        )
        .subscribe((user) => {
          if (user) {
            this.currentUserSubject.next(user);
          }
        });
    }
  }

  // Get current user
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // Update profile
  updateProfile(profileData: Partial<User>): Observable<User> {
    const headers = this.createAuthHeader();
    return this.http
      .put<User>(`${environment.apiUrl}/users/profile`, profileData, {
        headers,
      })
      .pipe(tap((user) => this.currentUserSubject.next(user)));
  }
}
```

### Step 1.6: HTTP Interceptor for Basic Auth

**src/app/core/interceptors/auth.interceptor.ts**

```typescript
import { Injectable } from "@angular/core";
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
} from "@angular/common/http";
import { AuthService } from "../services/auth.service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // Skip auth for public endpoints
    if (
      req.url.includes("/api/public") ||
      req.url.includes("/users/register")
    ) {
      return next.handle(req);
    }

    // Add Basic Auth header for API requests
    const credentials = this.authService.getStoredCredentials();
    if (credentials && req.url.includes("/api/")) {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Basic ${credentials}`,
        },
      });
      return next.handle(authReq);
    }

    return next.handle(req);
  }
}
```

### Step 1.7: Auth Guard

**src/app/core/guards/auth.guard.ts**

```typescript
import { Injectable } from "@angular/core";
import { CanActivate, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";

@Injectable({
  providedIn: "root",
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.authService.isAuthenticated()) {
      return true;
    }

    this.router.navigate(["/auth/login"]);
    return false;
  }
}
```

### Step 1.8: Login Component

**src/app/features/auth/login/login.component.ts**

```typescript
import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { NotificationService } from "../../../core/services/notification.service";

@Component({
  selector: "app-login",
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>Welcome to Travner</mat-card-title>
          <mat-card-subtitle
            >Sign in to share your travel experiences</mat-card-subtitle
          >
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" required />
              <mat-error
                *ngIf="loginForm.get('username')?.hasError('required')"
              >
                Username is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input
                matInput
                type="password"
                formControlName="password"
                required
              />
              <mat-error
                *ngIf="loginForm.get('password')?.hasError('required')"
              >
                Password is required
              </mat-error>
            </mat-form-field>

            <button
              mat-raised-button
              color="primary"
              type="submit"
              class="full-width login-button"
              [disabled]="loginForm.invalid || isLoading"
            >
              <mat-spinner diameter="20" *ngIf="isLoading"></mat-spinner>
              <span *ngIf="!isLoading">Sign In</span>
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p>
            Don't have an account?
            <a mat-button routerLink="/auth/register">Register here</a>
          </p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .auth-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 80vh;
        padding: 20px;
      }

      .auth-card {
        width: 100%;
        max-width: 400px;
      }

      .full-width {
        width: 100%;
        margin-bottom: 16px;
      }

      .login-button {
        height: 48px;
        margin-top: 16px;
      }
    `,
  ],
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {
    this.loginForm = this.fb.group({
      username: ["", Validators.required],
      password: ["", Validators.required],
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid && !this.isLoading) {
      this.isLoading = true;

      this.authService.login(this.loginForm.value).subscribe({
        next: (user) => {
          this.notificationService.showSuccess(
            `Welcome back, ${user.firstName}!`
          );
          this.router.navigate(["/posts"]);
        },
        error: (error) => {
          this.notificationService.showError("Invalid username or password");
          this.isLoading = false;
        },
        complete: () => {
          this.isLoading = false;
        },
      });
    }
  }
}
```

### Step 1.9: App Routing Configuration

**src/app/app-routing.module.ts**

```typescript
import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./core/guards/auth.guard";

const routes: Routes = [
  { path: "", redirectTo: "/posts", pathMatch: "full" },
  {
    path: "auth",
    loadChildren: () =>
      import("./features/auth/auth.module").then((m) => m.AuthModule),
  },
  {
    path: "posts",
    loadChildren: () =>
      import("./features/posts/posts.module").then((m) => m.PostsModule),
    canActivate: [AuthGuard],
  },
  {
    path: "chat",
    loadChildren: () =>
      import("./features/chat/chat.module").then((m) => m.ChatModule),
    canActivate: [AuthGuard],
  },
  {
    path: "profile",
    loadChildren: () =>
      import("./features/profile/profile.module").then((m) => m.ProfileModule),
    canActivate: [AuthGuard],
  },
  { path: "**", redirectTo: "/posts" },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
```

---

## 🎯 Phase 2: Travel Posts Module

**Time Estimate: 2-3 days**

### Step 2.1: Post Models

**src/app/core/models/post.model.ts**

```typescript
export interface Post {
  id: string;
  title: string;
  content: string;
  summary?: string;
  author: UserSummary;
  media: MediaItem[];
  tags: string[];
  location?: string;
  createdAt: Date;
  updatedAt: Date;
  commentCount: number;
}

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
}

export interface MediaItem {
  id: string;
  url: string;
  type: "IMAGE" | "VIDEO" | "DOCUMENT";
  filename: string;
  caption?: string;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  summary?: string;
  mediaIds: string[];
  tags: string[];
  location?: string;
}

export interface Comment {
  id: string;
  content: string;
  author: UserSummary;
  createdAt: Date;
  replies?: Comment[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
}
```

### Step 2.2: Posts Service

**src/app/features/posts/services/post.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../../environments/environment";
import {
  Post,
  CreatePostRequest,
  Comment,
  PagedResponse,
} from "../../../core/models/post.model";

@Injectable({
  providedIn: "root",
})
export class PostService {
  private apiUrl = `${environment.apiUrl}/posts`;

  constructor(private http: HttpClient) {}

  // Get paginated posts
  getPosts(
    page: number = 0,
    size: number = 10,
    search?: string
  ): Observable<PagedResponse<Post>> {
    let params = new HttpParams()
      .set("page", page.toString())
      .set("size", size.toString());

    if (search) {
      params = params.set("search", search);
    }

    return this.http.get<PagedResponse<Post>>(this.apiUrl, { params });
  }

  // Get single post
  getPost(id: string): Observable<Post> {
    return this.http.get<Post>(`${this.apiUrl}/${id}`);
  }

  // Create new post
  createPost(postData: CreatePostRequest): Observable<Post> {
    return this.http.post<Post>(this.apiUrl, postData);
  }

  // Update post
  updatePost(
    id: string,
    postData: Partial<CreatePostRequest>
  ): Observable<Post> {
    return this.http.put<Post>(`${this.apiUrl}/${id}`, postData);
  }

  // Delete post
  deletePost(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Get post comments
  getComments(postId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.apiUrl}/${postId}/comments`);
  }

  // Add comment
  addComment(postId: string, content: string): Observable<Comment> {
    return this.http.post<Comment>(`${this.apiUrl}/${postId}/comments`, {
      content,
    });
  }

  // Upload media
  uploadMedia(file: File): Observable<{ id: string; url: string }> {
    const formData = new FormData();
    formData.append("file", file);
    return this.http.post<{ id: string; url: string }>(
      `${environment.apiUrl}/media`,
      formData
    );
  }
}
```

### Step 2.3: Post List Component

**src/app/features/posts/post-list/post-list.component.ts**

```typescript
import { Component, OnInit } from "@angular/core";
import { Observable, BehaviorSubject } from "rxjs";
import { PostService } from "../services/post.service";
import { Post, PagedResponse } from "../../../core/models/post.model";

@Component({
  selector: "app-post-list",
  template: `
    <div class="posts-container">
      <!-- Header with search and create button -->
      <div class="posts-header">
        <h1>Travel Stories</h1>
        <div class="header-actions">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search posts...</mat-label>
            <input matInput [(ngModel)]="searchTerm" (input)="onSearch()" />
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>
          <button
            mat-fab
            color="primary"
            routerLink="/posts/create"
            class="create-button"
          >
            <mat-icon>add</mat-icon>
          </button>
        </div>
      </div>

      <!-- Posts grid -->
      <div class="posts-grid" *ngIf="!isLoading">
        <app-post-card
          *ngFor="let post of posts"
          [post]="post"
          (postDeleted)="onPostDeleted($event)"
        >
        </app-post-card>
      </div>

      <!-- Loading spinner -->
      <div class="loading-container" *ngIf="isLoading">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Load more button -->
      <div class="load-more-container" *ngIf="hasMore && !isLoading">
        <button
          mat-stroked-button
          (click)="loadMore()"
          class="load-more-button"
        >
          Load More Posts
        </button>
      </div>

      <!-- Empty state -->
      <div class="empty-state" *ngIf="posts.length === 0 && !isLoading">
        <mat-icon class="empty-icon">explore</mat-icon>
        <h2>No travel stories yet</h2>
        <p>Be the first to share your adventure!</p>
        <button mat-raised-button color="primary" routerLink="/posts/create">
          Create Your First Post
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .posts-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 20px;
      }

      .posts-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 30px;
        flex-wrap: wrap;
        gap: 16px;
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: 16px;
      }

      .search-field {
        width: 300px;
      }

      .posts-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 24px;
        margin-bottom: 30px;
      }

      .loading-container {
        display: flex;
        justify-content: center;
        padding: 40px;
      }

      .load-more-container {
        text-align: center;
        margin: 20px 0;
      }

      .load-more-button {
        padding: 12px 24px;
      }

      .empty-state {
        text-align: center;
        padding: 60px 20px;
      }

      .empty-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #666;
        margin-bottom: 16px;
      }

      .create-button {
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 1000;
      }

      @media (max-width: 768px) {
        .posts-header {
          flex-direction: column;
          align-items: stretch;
        }

        .header-actions {
          justify-content: space-between;
        }

        .search-field {
          flex: 1;
          min-width: 200px;
        }

        .posts-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class PostListComponent implements OnInit {
  posts: Post[] = [];
  isLoading = false;
  searchTerm = "";
  currentPage = 0;
  pageSize = 10;
  hasMore = true;

  constructor(private postService: PostService) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(reset: boolean = false): void {
    if (reset) {
      this.posts = [];
      this.currentPage = 0;
      this.hasMore = true;
    }

    this.isLoading = true;

    this.postService
      .getPosts(this.currentPage, this.pageSize, this.searchTerm || undefined)
      .subscribe({
        next: (response: PagedResponse<Post>) => {
          if (reset) {
            this.posts = response.content;
          } else {
            this.posts = [...this.posts, ...response.content];
          }
          this.hasMore = response.hasNext;
          this.isLoading = false;
        },
        error: (error) => {
          console.error("Error loading posts:", error);
          this.isLoading = false;
        },
      });
  }

  loadMore(): void {
    if (this.hasMore && !this.isLoading) {
      this.currentPage++;
      this.loadPosts();
    }
  }

  onSearch(): void {
    // Debounce search
    setTimeout(() => {
      this.loadPosts(true);
    }, 300);
  }

  onPostDeleted(postId: string): void {
    this.posts = this.posts.filter((post) => post.id !== postId);
  }
}
```

---

## 🎯 Phase 3: Real-time Chat System

**Time Estimate: 3-4 days**

### Step 3.1: Install WebSocket Dependencies

```bash
# Install WebSocket dependencies
npm install @stomp/ng2-stompjs @stomp/stompjs sockjs-client
npm install @types/sockjs-client --save-dev
```

### Step 3.2: Chat Models

**src/app/core/models/chat.model.ts**

```typescript
export interface Conversation {
  id: string;
  type: "DIRECT" | "GROUP";
  title?: string;
  members: ConversationMember[];
  ownerId: string;
  adminIds: string[];
  createdAt: Date;
  lastMessageAt: Date;
  isArchived: boolean;
  unreadCount: number;
  lastMessage?: Message;
}

export interface ConversationMember {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  isOnline: boolean;
  lastSeen: Date;
  role: "OWNER" | "ADMIN" | "MEMBER";
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  kind: "TEXT" | "IMAGE" | "FILE" | "SYSTEM";
  content: string;
  attachments: MessageAttachment[];
  replyToMessageId?: string;
  replyToMessage?: Message;
  sentAt: Date;
  editedAt?: Date;
  isEdited: boolean;
  readByUserIds: string[];
  readCount: number;
}

export interface MessageAttachment {
  mediaId: string;
  caption?: string;
  url: string;
  filename: string;
  size: number;
  type: "IMAGE" | "VIDEO" | "DOCUMENT";
}

export interface SendMessageRequest {
  conversationId: string;
  kind: "TEXT" | "IMAGE" | "FILE" | "SYSTEM";
  content: string;
  attachments?: MessageAttachment[];
  replyToMessageId?: string;
}

export interface CreateConversationRequest {
  type: "DIRECT" | "GROUP";
  title?: string;
  memberIds: string[];
}
```

### Step 3.3: WebSocket Service

**src/app/features/chat/services/websocket.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { Client, StompConfig } from "@stomp/stompjs";
import { BehaviorSubject, Observable, Subject } from "rxjs";
import { environment } from "../../../../environments/environment";
import { AuthService } from "../../../core/services/auth.service";
import { Message } from "../../../core/models/chat.model";

@Injectable({
  providedIn: "root",
})
export class WebSocketService {
  private stompClient: Client;
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private messagesSubject = new Subject<Message>();
  private typingSubject = new Subject<{
    conversationId: string;
    userId: string;
    isTyping: boolean;
  }>();

  public connectionStatus$ = this.connectionStatus.asObservable();
  public messages$ = this.messagesSubject.asObservable();
  public typing$ = this.typingSubject.asObservable();

  constructor(private authService: AuthService) {}

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      // For WebSocket, we need to get JWT token
      // Since backend uses JWT for WebSocket, you'll need to implement JWT endpoint
      const jwtToken = localStorage.getItem(environment.jwtTokenKey);

      const config: StompConfig = {
        brokerURL: environment.wsUrl,
        connectHeaders: {
          Authorization: `Bearer ${jwtToken}`,
        },
        debug: (str) => {
          console.log("WebSocket Debug:", str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      };

      this.stompClient = new Client(config);

      this.stompClient.onConnect = () => {
        console.log("WebSocket Connected");
        this.connectionStatus.next(true);
        this.subscribeToPersonalQueue();
        resolve();
      };

      this.stompClient.onStompError = (frame) => {
        console.error("WebSocket Error:", frame);
        this.connectionStatus.next(false);
        reject(frame);
      };

      this.stompClient.onDisconnect = () => {
        console.log("WebSocket Disconnected");
        this.connectionStatus.next(false);
      };

      this.stompClient.activate();
    });
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.connectionStatus.next(false);
    }
  }

  private subscribeToPersonalQueue(): void {
    this.stompClient.subscribe("/user/queue/messages", (message) => {
      const messageData: Message = JSON.parse(message.body);
      this.messagesSubject.next(messageData);
    });
  }

  subscribeToConversation(conversationId: string): void {
    this.stompClient.subscribe(
      `/topic/conversations/${conversationId}`,
      (message) => {
        const messageData: Message = JSON.parse(message.body);
        this.messagesSubject.next(messageData);
      }
    );
  }

  sendMessage(message: any): void {
    if (this.stompClient && this.connectionStatus.value) {
      this.stompClient.publish({
        destination: "/app/chat.sendMessage",
        body: JSON.stringify(message),
      });
    }
  }

  sendTypingIndicator(conversationId: string, isTyping: boolean): void {
    if (this.stompClient && this.connectionStatus.value) {
      this.stompClient.publish({
        destination: "/app/chat.typing",
        body: JSON.stringify({
          conversationId,
          isTyping,
        }),
      });
    }
  }

  isConnected(): boolean {
    return this.connectionStatus.value;
  }
}
```

### Step 3.4: Chat Service

**src/app/features/chat/services/chat.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, BehaviorSubject } from "rxjs";
import { tap } from "rxjs/operators";
import { environment } from "../../../../environments/environment";
import {
  Conversation,
  Message,
  CreateConversationRequest,
  SendMessageRequest,
  PagedResponse,
} from "../../../core/models/chat.model";

@Injectable({
  providedIn: "root",
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat`;
  private conversationsSubject = new BehaviorSubject<Conversation[]>([]);

  public conversations$ = this.conversationsSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Get user conversations
  getConversations(): Observable<Conversation[]> {
    return this.http
      .get<Conversation[]>(`${this.apiUrl}/conversations`)
      .pipe(
        tap((conversations) => this.conversationsSubject.next(conversations))
      );
  }

  // Get conversation details
  getConversation(id: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiUrl}/conversations/${id}`);
  }

  // Create new conversation
  createConversation(
    conversationData: CreateConversationRequest
  ): Observable<Conversation> {
    return this.http
      .post<Conversation>(`${this.apiUrl}/conversations`, conversationData)
      .pipe(
        tap((newConversation) => {
          const currentConversations = this.conversationsSubject.value;
          this.conversationsSubject.next([
            newConversation,
            ...currentConversations,
          ]);
        })
      );
  }

  // Get conversation messages
  getMessages(
    conversationId: string,
    page: number = 0,
    size: number = 50
  ): Observable<PagedResponse<Message>> {
    const params = new HttpParams()
      .set("page", page.toString())
      .set("size", size.toString());

    return this.http.get<PagedResponse<Message>>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      { params }
    );
  }

  // Send message via HTTP (for attachments)
  sendMessage(messageData: SendMessageRequest): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/messages`, messageData);
  }

  // Edit message
  editMessage(messageId: string, content: string): Observable<Message> {
    return this.http.put<Message>(`${this.apiUrl}/messages/${messageId}`, {
      content,
    });
  }

  // Delete message
  deleteMessage(messageId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/messages/${messageId}`);
  }

  // Mark messages as read
  markAsRead(messageIds: string[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/messages/read`, { messageIds });
  }

  // Add members to conversation
  addMembers(
    conversationId: string,
    memberIds: string[]
  ): Observable<Conversation> {
    return this.http.post<Conversation>(
      `${this.apiUrl}/conversations/${conversationId}/members`,
      { memberIds }
    );
  }

  // Remove member from conversation
  removeMember(conversationId: string, userId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/conversations/${conversationId}/members/${userId}`
    );
  }

  // Update local conversations list
  updateConversation(conversation: Conversation): void {
    const conversations = this.conversationsSubject.value;
    const index = conversations.findIndex((c) => c.id === conversation.id);
    if (index >= 0) {
      conversations[index] = conversation;
      this.conversationsSubject.next([...conversations]);
    }
  }
}
```

---

## 🎯 Phase 4: UI Components & Styling

**Time Estimate: 2-3 days**

### Step 4.1: Material Theme Configuration

**src/styles.scss**

```scss
@import "@angular/material/theming";
@include mat-core();

// Travel-inspired color palette
$travner-primary: mat-palette($mat-blue, 600, 400, 800);
$travner-accent: mat-palette($mat-orange, 500, 300, 700);
$travner-warn: mat-palette($mat-red);

$travner-theme: mat-light-theme(
  $travner-primary,
  $travner-accent,
  $travner-warn
);
@include angular-material-theme($travner-theme);

// Custom CSS variables
:root {
  --primary-blue: #1976d2;
  --accent-orange: #ff9800;
  --light-bg: #f8f9fa;
  --white: #ffffff;
  --border-radius: 12px;
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.1);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.15);
  --transition: all 0.3s ease;
}

// Global styles
body {
  font-family: "Inter", "Roboto", sans-serif;
  margin: 0;
  background-color: var(--light-bg);
  line-height: 1.6;
}

// Utility classes
.full-width {
  width: 100%;
}

.text-center {
  text-align: center;
}

.mt-16 {
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.p-16 {
  padding: 16px;
}

// Custom scrollbar
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

// Responsive breakpoints
@media (max-width: 768px) {
  .hide-mobile {
    display: none !important;
  }
}

@media (min-width: 769px) {
  .hide-desktop {
    display: none !important;
  }
}
```

### Step 4.2: Main Navigation Component

**src/app/shared/components/navbar/navbar.component.ts**

```typescript
import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { Observable } from "rxjs";
import { AuthService } from "../../../core/services/auth.service";
import { User } from "../../../core/models/user.model";

@Component({
  selector: "app-navbar",
  template: `
    <mat-toolbar color="primary" class="navbar">
      <div class="navbar-content">
        <!-- Logo and brand -->
        <div class="brand" routerLink="/posts">
          <mat-icon class="brand-icon">flight_takeoff</mat-icon>
          <span class="brand-text">Travner</span>
        </div>

        <!-- Desktop navigation -->
        <nav class="nav-links hide-mobile">
          <a mat-button routerLink="/posts" routerLinkActive="active">
            <mat-icon>explore</mat-icon>
            Posts
          </a>
          <a mat-button routerLink="/chat" routerLinkActive="active">
            <mat-icon>chat</mat-icon>
            Chat
          </a>
          <a mat-button routerLink="/profile" routerLinkActive="active">
            <mat-icon>person</mat-icon>
            Profile
          </a>
        </nav>

        <!-- User menu -->
        <div class="user-menu" *ngIf="currentUser$ | async as user">
          <button
            mat-icon-button
            [matMenuTriggerFor]="userMenuRef"
            class="user-avatar"
          >
            <img [src]="getUserAvatar(user)" [alt]="user.firstName" />
          </button>

          <mat-menu #userMenuRef="matMenu">
            <div class="user-info">
              <div class="user-name">
                {{ user.firstName }} {{ user.lastName }}
              </div>
              <div class="user-email">{{ user.email }}</div>
            </div>
            <mat-divider></mat-divider>
            <button mat-menu-item routerLink="/profile">
              <mat-icon>person</mat-icon>
              Profile
            </button>
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon>
              Logout
            </button>
          </mat-menu>
        </div>

        <!-- Mobile menu -->
        <button
          mat-icon-button
          class="mobile-menu-button hide-desktop"
          [matMenuTriggerFor]="mobileMenuRef"
        >
          <mat-icon>menu</mat-icon>
        </button>

        <mat-menu #mobileMenuRef="matMenu">
          <a mat-menu-item routerLink="/posts">
            <mat-icon>explore</mat-icon>
            Posts
          </a>
          <a mat-menu-item routerLink="/chat">
            <mat-icon>chat</mat-icon>
            Chat
          </a>
          <a mat-menu-item routerLink="/profile">
            <mat-icon>person</mat-icon>
            Profile
          </a>
          <mat-divider></mat-divider>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon>
            Logout
          </button>
        </mat-menu>
      </div>
    </mat-toolbar>
  `,
  styles: [
    `
      .navbar {
        position: sticky;
        top: 0;
        z-index: 1000;
        box-shadow: var(--shadow-sm);
      }

      .navbar-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        max-width: 1200px;
        margin: 0 auto;
      }

      .brand {
        display: flex;
        align-items: center;
        cursor: pointer;
        text-decoration: none;
        color: inherit;
      }

      .brand-icon {
        margin-right: 8px;
        font-size: 28px;
      }

      .brand-text {
        font-size: 24px;
        font-weight: 600;
      }

      .nav-links {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .nav-links a {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .nav-links a.active {
        background-color: rgba(255, 255, 255, 0.1);
      }

      .user-menu {
        display: flex;
        align-items: center;
      }

      .user-avatar img {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        object-fit: cover;
      }

      .user-info {
        padding: 16px;
        min-width: 200px;
      }

      .user-name {
        font-weight: 600;
        font-size: 16px;
      }

      .user-email {
        color: #666;
        font-size: 14px;
      }

      @media (max-width: 768px) {
        .brand-text {
          display: none;
        }
      }
    `,
  ],
})
export class NavbarComponent {
  currentUser$: Observable<User | null>;

  constructor(private authService: AuthService, private router: Router) {
    this.currentUser$ = this.authService.currentUser$;
  }

  getUserAvatar(user: User): string {
    return (
      user.avatar ||
      `https://ui-avatars.com/api/?name=${user.firstName}+${user.lastName}&background=1976d2&color=fff`
    );
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(["/auth/login"]);
  }
}
```

---

## 🎯 Phase 5: Final Integration & Testing

**Time Estimate: 1-2 days**

### Step 5.1: App Module Configuration

**src/app/app.module.ts**

```typescript
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientModule, HTTP_INTERCEPTORS } from "@angular/common/http";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";

// Material modules
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { MatChipsModule } from "@angular/material/chips";
import { MatDialogModule } from "@angular/material/dialog";

// Toast notifications
import { ToastrModule } from "ngx-toastr";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";

// Core modules
import { CoreModule } from "./core/core.module";
import { SharedModule } from "./shared/shared.module";

// Interceptors
import { AuthInterceptor } from "./core/interceptors/auth.interceptor";
import { ErrorInterceptor } from "./core/interceptors/error.interceptor";

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule,

    // Material modules
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDialogModule,

    // Toast notifications
    ToastrModule.forRoot({
      timeOut: 3000,
      positionClass: "toast-top-right",
      preventDuplicates: true,
    }),

    // Core modules
    CoreModule,
    SharedModule,
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
```

### Step 5.2: Error Handling Interceptor

**src/app/core/interceptors/error.interceptor.ts**

```typescript
import { Injectable } from "@angular/core";
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpErrorResponse,
} from "@angular/common/http";
import { Router } from "@angular/router";
import { catchError } from "rxjs/operators";
import { throwError } from "rxjs";
import { AuthService } from "../services/auth.service";
import { NotificationService } from "../services/notification.service";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = "An unexpected error occurred";

        if (error.status === 401) {
          // Unauthorized - redirect to login
          this.authService.logout();
          this.router.navigate(["/auth/login"]);
          errorMessage = "Please log in to continue";
        } else if (error.status === 403) {
          errorMessage = "You do not have permission to perform this action";
        } else if (error.status === 404) {
          errorMessage = "The requested resource was not found";
        } else if (error.status === 500) {
          errorMessage = "Server error. Please try again later";
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        }

        this.notificationService.showError(errorMessage);
        return throwError(() => error);
      })
    );
  }
}
```

### Step 5.3: Notification Service

**src/app/core/services/notification.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { ToastrService } from "ngx-toastr";

@Injectable({
  providedIn: "root",
})
export class NotificationService {
  constructor(private toastr: ToastrService) {}

  showSuccess(message: string, title?: string): void {
    this.toastr.success(message, title || "Success");
  }

  showError(message: string, title?: string): void {
    this.toastr.error(message, title || "Error");
  }

  showInfo(message: string, title?: string): void {
    this.toastr.info(message, title || "Info");
  }

  showWarning(message: string, title?: string): void {
    this.toastr.warning(message, title || "Warning");
  }
}
```

### Step 5.4: Main App Component

**src/app/app.component.ts**

```typescript
import { Component, OnInit } from "@angular/core";
import { AuthService } from "./core/services/auth.service";
import { WebSocketService } from "./features/chat/services/websocket.service";

@Component({
  selector: "app-root",
  template: `
    <div class="app-container">
      <app-navbar *ngIf="isAuthenticated"></app-navbar>
      <main class="main-content" [class.with-navbar]="isAuthenticated">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    `
      .app-container {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
      }

      .main-content {
        flex: 1;
      }

      .main-content.with-navbar {
        padding-top: 0;
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  title = "travner-frontend";
  isAuthenticated = false;

  constructor(
    private authService: AuthService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    // Subscribe to authentication status
    this.authService.currentUser$.subscribe((user) => {
      this.isAuthenticated = !!user;

      // Connect to WebSocket when user is authenticated
      if (user && !this.webSocketService.isConnected()) {
        this.webSocketService.connect().catch(console.error);
      } else if (!user && this.webSocketService.isConnected()) {
        this.webSocketService.disconnect();
      }
    });
  }
}
```

---

## 🚀 Deployment & Production Setup

### Build Configuration

**angular.json** (production configuration)

```json
"production": {
  "budgets": [
    {
      "type": "initial",
      "maximumWarning": "2mb",
      "maximumError": "5mb"
    }
  ],
  "fileReplacements": [
    {
      "replace": "src/environments/environment.ts",
      "with": "src/environments/environment.prod.ts"
    }
  ],
  "outputHashing": "all",
  "sourceMap": false,
  "namedChunks": false,
  "extractLicenses": true,
  "vendorChunk": false,
  "buildOptimizer": true,
  "optimization": true,
  "aot": true
}
```

### Production Environment

**src/environments/environment.prod.ts**

```typescript
export const environment = {
  production: true,
  apiUrl: "https://your-backend-domain.com/api",
  wsUrl: "wss://your-backend-domain.com/ws",
  fileUploadMaxSize: 20 * 1024 * 1024,
  credentialsKey: "travner_credentials",
  jwtTokenKey: "travner_jwt_token",
};
```

### Build Commands

```bash
# Development build
ng build

# Production build
ng build --configuration production

# Serve locally
ng serve

# Run tests
ng test

# Run e2e tests
ng e2e
```

---

## ✅ Implementation Checklist

### Phase 1: Foundation ✅

- [ ] Angular project setup with Material Design
- [ ] Basic Authentication implementation
- [ ] Route guards and HTTP interceptors
- [ ] User registration and login forms
- [ ] Environment configuration
- [ ] Error handling and notifications

### Phase 2: Posts Module ✅

- [ ] Post creation with rich text editor
- [ ] Post list with pagination
- [ ] File upload functionality
- [ ] Comment system
- [ ] Responsive post cards
- [ ] Search and filtering

### Phase 3: Chat System ✅

- [ ] WebSocket connection setup
- [ ] Real-time messaging
- [ ] Conversation management
- [ ] Typing indicators
- [ ] File attachments in chat
- [ ] Message read receipts

### Phase 4: UI Polish ✅

- [ ] Material Design theming
- [ ] Responsive navigation
- [ ] Loading states and animations
- [ ] Mobile-first responsive design
- [ ] User avatar and profile display

### Phase 5: Production Ready ✅

- [ ] Error handling and recovery
- [ ] Performance optimizations
- [ ] Build configuration
- [ ] Environment setup
- [ ] Testing implementation

---

## 🔧 Key Implementation Notes

1. **Authentication**: Use Basic Auth for all REST API calls, JWT only for WebSocket connections
2. **State Management**: Use services with BehaviorSubjects for reactive state management
3. **Responsive Design**: Mobile-first approach with Angular Flex Layout
4. **Error Handling**: Comprehensive error interceptor with user-friendly messages
5. **Performance**: Lazy loading modules, OnPush change detection where applicable
6. **WebSocket**: Automatic reconnection and proper cleanup
7. **File Uploads**: Progress indicators and size validation

---

## 📚 Additional Resources

- **Angular Material Documentation**: https://material.angular.io/
- **Angular WebSocket Guide**: https://angular.io/guide/service-worker-communications
- **STOMP.js Documentation**: https://stomp-js.github.io/
- **Angular Best Practices**: https://angular.io/guide/styleguide

This guide provides a complete roadmap for building a production-ready Angular frontend that perfectly integrates with your Travner backend. Each phase builds upon the previous one, ensuring a solid foundation and professional result.
