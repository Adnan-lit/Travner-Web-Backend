# Travner Social Media Post System

### Overview

The implemented post system allows travelers to share their travel experiences through posts, images, videos, and interactive comments.

[![Run in Postman](https://run.pstmn.io/button.svg)](https://www.postman.com/travner/collection/travner-social-api)

### Features

1. **Post Management**

   - Create, view, update, and delete travel posts
   - Search posts by location, title, content, or tags
   - Filter posts by user or tags
   - Support for upvoting and downvoting posts

2. **Media Support**

   - Upload images and videos up to 20MB
   - Multiple media files per post
   - Secure storage in MongoDB GridFS
   - Accessible through dedicated API endpoints

3. **Comment System**

   - Nested comments (replies to comments)
   - Upvote/downvote functionality
   - Pagination for large comment threads

4. **Security**
   - Authentication required for creating/editing posts and comments
   - Public access for viewing posts and comments
   - Authorization checks for updates and deletions

### API Endpoints

#### Posts

- `GET /posts` - Get all published posts (paginated)
- `GET /posts/{id}` - Get a specific post by ID
- `GET /posts/user/{username}` - Get posts by a specific user
- `GET /posts/search?query=...` - Search posts
- `GET /posts/location?location=...` - Get posts by location
- `GET /posts/tags?tags=...` - Get posts by tags
- `POST /posts` - Create a new post
- `PUT /posts/{id}` - Update a post
- `DELETE /posts/{id}` - Delete a post
- `POST /posts/{id}/upvote` - Upvote a post
- `POST /posts/{id}/downvote` - Downvote a post

#### Comments

- `GET /posts/{postId}/comments` - Get comments for a post
- `GET /posts/{postId}/comments/{id}` - Get a specific comment
- `POST /posts/{postId}/comments` - Create a comment
- `PUT /posts/{postId}/comments/{id}` - Update a comment
- `DELETE /posts/{postId}/comments/{id}` - Delete a comment
- `POST /posts/{postId}/comments/{id}/upvote` - Upvote a comment
- `POST /posts/{postId}/comments/{id}/downvote` - Downvote a comment

#### Media

- `GET /posts/{postId}/media` - Get media for a post
- `POST /posts/{postId}/media/upload` - Upload media for a post
- `DELETE /posts/{postId}/media/{id}` - Delete media

### Configuration

The system uses a configurable upload directory specified in `application.yml` and supports environment variable overrides for flexible deployment.

### Testing with Postman

Below are examples of how to test the API endpoints using Postman:

#### Authentication Setup

Since the API uses Basic Authentication:

1. In Postman, go to the "Authorization" tab
2. Select "Basic Auth" type
3. Enter your username and password
4. This authentication should be included in all requests that require authentication

#### Example Requests

##### 1. Create a new post

- Method: POST
- URL: `http://localhost:8080/posts`
- Authorization: Basic Auth
- Headers:
  - Content-Type: application/json
- Body (raw JSON):

```json
{
  "title": "Amazing Trip to Bali",
  "content": "I spent two incredible weeks exploring the beaches and temples of Bali...",
  "location": "Bali, Indonesia",
  "tags": ["beach", "temple", "island", "vacation"],
  "published": true
}
```

##### 2. Upload media to a post

- Method: POST
- URL: `http://localhost:8080/posts/{postId}/media/upload`
- Authorization: Basic Auth
- Body (form-data):
  - Key: file
  - Value: [Select file]
  - Type: File

##### 3. Add a comment to a post

- Method: POST
- URL: `http://localhost:8080/posts/{postId}/comments`
- Authorization: Basic Auth
- Headers:
  - Content-Type: application/json
- Body (raw JSON):

```json
{
  "content": "Great post! What was your favorite beach in Bali?"
}
```

##### 4. Reply to a comment

- Method: POST
- URL: `http://localhost:8080/posts/{postId}/comments`
- Authorization: Basic Auth
- Headers:
  - Content-Type: application/json
- Body (raw JSON):

```json
{
  "content": "I also loved the temples there!",
  "parentCommentId": "{commentId}"
}
```

##### 5. Search posts by location

- Method: GET
- URL: `http://localhost:8080/posts/location?location=Bali&page=0&size=10`
- No authentication needed (public endpoint)

##### 6. Upvote a post

- Method: POST
- URL: `http://localhost:8080/posts/{postId}/upvote`
- Authorization: Basic Auth

##### 7. Get all comments for a post

- Method: GET
- URL: `http://localhost:8080/posts/{postId}/comments?page=0&size=20`
- No authentication needed (public endpoint)

### Troubleshooting

If you encounter issues with posts not appearing in the system, check the following:

#### 1. Ensure Posts Are Published

Posts must have the `published` field set to `true` to appear in public feeds. If a post is not appearing:

```json
// Check that your post creation request includes:
{
  // other fields...
  "published": true
}
```

#### 2. Use Debug Endpoints (Admin Only)

These endpoints help diagnose issues with posts:

- `GET /debug/post-count` - Check total posts and published posts count
- `POST /debug/create-test-post` - Create a test post that's guaranteed to be published
- `POST /debug/fix-posts` - Fix all posts by setting them to published state

#### 3. Common Issues and Solutions

- **Empty post results**: Ensure that posts have `published` set to true
- **Media uploads failing**: Check the file size (max 20MB) and ensure your MongoDB connection is working properly
- **Comments not appearing**: Verify the post ID is correct and the parent comment ID for replies

### MongoDB GridFS Media Storage

The system now stores media files directly in MongoDB using GridFS, which offers several benefits:

1. **Simplified Deployment**: No need to configure separate file storage systems
2. **Cloud Compatibility**: Works seamlessly in containerized environments
3. **Database Integration**: Files are backed up with regular database backups
4. **Scalability**: Automatically handles large files by splitting into chunks

#### 4. Important Notes

- Boolean field in MongoDB is stored as `published` (not `isPublished`)
- Comment counts are calculated dynamically, not stored on the post
- All list endpoints support pagination to improve performance
