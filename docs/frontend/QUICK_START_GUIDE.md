# 🚀 Travner Frontend - Quick Start Guide

## For AI Agents - Essential Commands & Setup

---

## 📦 Immediate Setup (5 minutes)

```bash
# 1. Create Angular project
ng new travner-frontend --routing --style=scss --skip-git
cd travner-frontend

# 2. Install all dependencies
npm install @angular/material @angular/cdk @angular/animations @angular/flex-layout ngx-toastr @stomp/ng2-stompjs @stomp/stompjs sockjs-client @types/sockjs-client

# 3. Add Material Design
ng add @angular/material --theme=indigo-pink --typography=true --animations=true

# 4. Generate project structure
ng generate module core
ng generate module shared
ng generate module features/auth --routing
ng generate module features/posts --routing
ng generate module features/chat --routing
ng generate service core/services/auth
ng generate service core/services/api
ng generate guard core/guards/auth
ng generate interceptor core/interceptors/auth
ng generate component features/auth/login
ng generate component features/auth/register
ng generate component shared/components/navbar
```

---

## ⚡ Critical Configuration Files

### 1. Environment Setup

**src/environments/environment.ts**

```typescript
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080/api",
  wsUrl: "ws://localhost:8080/ws",
  credentialsKey: "travner_credentials", // Basic Auth storage
  jwtTokenKey: "travner_jwt_token", // WebSocket JWT only
};
```

### 2. Authentication Service Core

**src/app/core/services/auth.service.ts**

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { BehaviorSubject, Observable } from "rxjs";
import { tap } from "rxjs/operators";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: "root" })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Basic Auth Login
  login(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({
      Authorization: `Basic ${btoa(
        credentials.username + ":" + credentials.password
      )}`,
    });

    return this.http
      .get<any>(`${environment.apiUrl}/users/profile`, { headers })
      .pipe(
        tap((user) => {
          this.currentUserSubject.next(user);
          localStorage.setItem(
            environment.credentialsKey,
            btoa(`${credentials.username}:${credentials.password}`)
          );
        })
      );
  }

  // Get stored credentials for API calls
  getStoredCredentials(): string | null {
    return localStorage.getItem(environment.credentialsKey);
  }

  // Logout
  logout(): void {
    localStorage.removeItem(environment.credentialsKey);
    this.currentUserSubject.next(null);
  }

  // Check authentication
  isAuthenticated(): boolean {
    return !!this.getStoredCredentials() && !!this.currentUserSubject.value;
  }
}
```

### 3. HTTP Interceptor for Auto Basic Auth

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
    // Skip for public endpoints
    if (
      req.url.includes("/api/public") ||
      req.url.includes("/users/register")
    ) {
      return next.handle(req);
    }

    // Add Basic Auth header
    const credentials = this.authService.getStoredCredentials();
    if (credentials && req.url.includes("/api/")) {
      const authReq = req.clone({
        setHeaders: { Authorization: `Basic ${credentials}` },
      });
      return next.handle(authReq);
    }

    return next.handle(req);
  }
}
```

---

## 🎯 Essential API Integration

### Posts Service Template

```typescript
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: "root" })
export class PostService {
  private apiUrl = `${environment.apiUrl}/posts`;

  constructor(private http: HttpClient) {}

  // All methods automatically get Basic Auth from interceptor
  getPosts(page = 0, size = 10): Observable<any> {
    return this.http.get(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  createPost(postData: any): Observable<any> {
    return this.http.post(this.apiUrl, postData);
  }

  uploadMedia(file: File): Observable<any> {
    const formData = new FormData();
    formData.append("file", file);
    return this.http.post(`${environment.apiUrl}/media`, formData);
  }
}
```

### Chat Service Template

```typescript
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: "root" })
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat`;

  constructor(private http: HttpClient) {}

  getConversations(): Observable<any> {
    return this.http.get(`${this.apiUrl}/conversations`);
  }

  getMessages(conversationId: string): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/conversations/${conversationId}/messages`
    );
  }

  sendMessage(messageData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/messages`, messageData);
  }
}
```

---

## 🔒 Authentication Flow

### 1. Register Component

```typescript
// POST to /api/users/register (no auth needed)
register(userData: any): Observable<any> {
  return this.http.post(`${environment.apiUrl}/users/register`, userData);
}
```

### 2. Login Component

```typescript
// Validate credentials against /api/users/profile with Basic Auth
onLogin() {
  this.authService.login(this.loginForm.value).subscribe({
    next: (user) => this.router.navigate(['/posts']),
    error: (error) => this.showError('Invalid credentials')
  });
}
```

### 3. Protected Routes

```typescript
const routes: Routes = [
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
];
```

---

## 🎨 Quick UI Setup

### App Module Setup

```typescript
import { HTTP_INTERCEPTORS } from "@angular/common/http";
import { AuthInterceptor } from "./core/interceptors/auth.interceptor";

@NgModule({
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
})
export class AppModule {}
```

### Material Design Theme

```scss
// src/styles.scss
@import "@angular/material/theming";
@include mat-core();

$primary: mat-palette($mat-blue, 600);
$accent: mat-palette($mat-orange, 500);
$theme: mat-light-theme($primary, $accent);
@include angular-material-theme($theme);

body {
  margin: 0;
  font-family: Roboto, sans-serif;
}
```

---

## 💬 WebSocket Integration (For Chat Only)

### WebSocket Service

```typescript
import { Client } from "@stomp/stompjs";

@Injectable({ providedIn: "root" })
export class WebSocketService {
  private stompClient: Client;

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      // Get JWT token for WebSocket (separate from Basic Auth)
      const jwtToken = localStorage.getItem(environment.jwtTokenKey);

      this.stompClient = new Client({
        brokerURL: environment.wsUrl,
        connectHeaders: { Authorization: `Bearer ${jwtToken}` },
        onConnect: () => resolve(),
        onStompError: (error) => reject(error),
      });

      this.stompClient.activate();
    });
  }

  sendMessage(destination: string, body: any): void {
    this.stompClient.publish({ destination, body: JSON.stringify(body) });
  }
}
```

---

## ✅ Development Checklist

### Phase 1: Core Setup (Day 1)

- [ ] Angular project with Material Design
- [ ] Basic Auth service and interceptor
- [ ] Login/Register components
- [ ] Route guards and navigation

### Phase 2: Posts Module (Day 2-3)

- [ ] Post list and create components
- [ ] File upload functionality
- [ ] Comment system
- [ ] Responsive design

### Phase 3: Chat System (Day 3-4)

- [ ] Chat layout and conversation list
- [ ] Real-time messaging with WebSocket
- [ ] Message input and display
- [ ] Typing indicators

### Phase 4: Polish (Day 5)

- [ ] Error handling and loading states
- [ ] Final responsive adjustments
- [ ] Performance optimizations
- [ ] Production build configuration

---

## 🚨 Critical Notes

1. **Authentication**: REST APIs use Basic Auth, WebSocket uses JWT
2. **Interceptor**: Automatically adds Basic Auth to all `/api/` requests
3. **Storage**: Store Basic Auth credentials, separate JWT for WebSocket
4. **Error Handling**: 401 responses should trigger logout and redirect
5. **CORS**: Backend already configured for frontend domain

---

## 🔧 Common Commands

```bash
# Start development server
ng serve

# Generate new component
ng generate component features/posts/post-card

# Generate new service
ng generate service features/posts/services/post

# Build for production
ng build --prod

# Run tests
ng test
```

This quick-start guide gives you everything needed to build the Travner frontend efficiently. Follow the phases in order for best results!
