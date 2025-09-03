# Travner API Documentation for Frontend Integration

## Overview

This document provides comprehensive information for frontend developers integrating with the Travner API. The API follows REST principles and uses JSON for request and response bodies.

## Base URL

```
http://localhost:8080
```

## Authentication

The API uses Basic Authentication. Include an `Authorization` header with each request that requires authentication.

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

## Standard Response Format

All API endpoints return a consistent response format:

```json
{
  "success": true,
  "message": "Success message",
  "data": {
    /* Response data */
  },
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

- `success`: Boolean indicating if the request was successful
- `message`: A human-readable message
- `data`: The actual response data (array or object)
- `pagination`: For paginated responses, contains metadata about pagination

## Error Responses

Error responses follow the same format:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

## Posts API

### Get All Posts

Retrieve a paginated list of all published posts.

**Endpoint**: `GET /posts`

**Query Parameters**:

- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size
- `sortBy` (default: "createdAt"): Field to sort by
- `direction` (default: "desc"): Sort direction ("asc" or "desc")

**Response**:

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "68b7a8a25035a56543b201ca",
      "title": "Amazing Trip to Bali",
      "content": "I spent two incredible weeks exploring...",
      "location": "Bali, Indonesia",
      "mediaUrls": [
        "/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb"
      ],
      "author": {
        "id": "68b7a8a25035a56543b201c9",
        "userName": "traveler123",
        "firstName": "John",
        "lastName": "Doe"
      },
      "createdAt": "2023-09-15T10:30:45",
      "updatedAt": "2023-09-15T10:30:45",
      "tags": ["beach", "vacation", "island"],
      "upvotes": 5,
      "downvotes": 0,
      "commentCount": 3,
      "published": true
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### Get Post by ID

Retrieve a specific post by its ID.

**Endpoint**: `GET /posts/{id}`

**Response**:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "68b7a8a25035a56543b201ca",
    "title": "Amazing Trip to Bali",
    "content": "I spent two incredible weeks exploring...",
    "location": "Bali, Indonesia",
    "mediaUrls": [
      "/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb"
    ],
    "author": {
      "id": "68b7a8a25035a56543b201c9",
      "userName": "traveler123",
      "firstName": "John",
      "lastName": "Doe"
    },
    "createdAt": "2023-09-15T10:30:45",
    "updatedAt": "2023-09-15T10:30:45",
    "tags": ["beach", "vacation", "island"],
    "upvotes": 5,
    "downvotes": 0,
    "commentCount": 3,
    "published": true
  }
}
```

### Get Posts by User

Retrieve posts created by a specific user.

**Endpoint**: `GET /posts/user/{username}`

**Query Parameters**:

- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts

### Search Posts

Search for posts based on a query term.

**Endpoint**: `GET /posts/search`

**Query Parameters**:

- `query`: Search term to match against post title and content
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts

### Get Posts by Location

Retrieve posts for a specific location.

**Endpoint**: `GET /posts/location`

**Query Parameters**:

- `location`: Location name to match (full or partial)
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts

### Get Posts by Tags

Retrieve posts that contain specific tags.

**Endpoint**: `GET /posts/tags`

**Query Parameters**:

- `tags`: List of tags to filter by (can be specified multiple times)
- `page` (default: 0): Zero-based page index
- `size` (default: 10): Page size

**Response**: Same format as Get All Posts

### Create Post

Create a new post. Requires authentication.

**Endpoint**: `POST /posts`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <base64-encoded-credentials>
```

**Request Body**:

```json
{
  "title": "My Adventure in Thailand",
  "content": "This was an amazing trip...",
  "location": "Bangkok, Thailand",
  "tags": ["adventure", "thailand", "travel"],
  "published": true
}
```

**Response**:

```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "68b7a8a25035a56543b201cd",
    "title": "My Adventure in Thailand",
    "content": "This was an amazing trip...",
    "location": "Bangkok, Thailand",
    "mediaUrls": [],
    "author": {
      "id": "68b7a8a25035a56543b201c9",
      "userName": "traveler123",
      "firstName": "John",
      "lastName": "Doe"
    },
    "createdAt": "2023-09-15T10:30:45",
    "updatedAt": "2023-09-15T10:30:45",
    "tags": ["adventure", "thailand", "travel"],
    "upvotes": 0,
    "downvotes": 0,
    "commentCount": 0,
    "published": true
  }
}
```

### Update Post

Update an existing post. Requires authentication and the user must be the original post author.

**Endpoint**: `PUT /posts/{id}`

**Headers**:

```
Content-Type: application/json
Authorization: Basic <base64-encoded-credentials>
```

**Request Body**: Same as Create Post

**Response**: Same as Create Post but with "Post updated successfully" message

### Delete Post

Delete a post. Requires authentication and the user must be the original post author.

**Endpoint**: `DELETE /posts/{id}`

**Headers**:

```
Authorization: Basic <base64-encoded-credentials>
```

**Response**:

```json
{
  "success": true,
  "message": "Post deleted successfully",
  "data": null
}
```

### Upvote Post

Upvote a post. Requires authentication.

**Endpoint**: `POST /posts/{id}/upvote`

**Headers**:

```
Authorization: Basic <base64-encoded-credentials>
```

**Response**:

```json
{
  "success": true,
  "message": "Post upvoted successfully",
  "data": {
    /* Post data with updated upvote count */
  }
}
```

### Downvote Post

Downvote a post. Requires authentication.

**Endpoint**: `POST /posts/{id}/downvote`

**Headers**:

```
Authorization: Basic <base64-encoded-credentials>
```

**Response**: Same as upvote but with "Post downvoted successfully" message

## Media API

### Get All Media for a Post

Retrieve all media associated with a specific post.

**Endpoint**: `GET /posts/{postId}/media`

**Response**:

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "68b7a8a25035a56543b201cb",
      "fileName": "beach.jpg",
      "fileUrl": "/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb",
      "fileType": "image/jpeg",
      "fileSize": 2048576,
      "uploaderId": "68b7a8a25035a56543b201c9",
      "postId": "68b7a8a25035a56543b201ca",
      "uploadedAt": "2023-09-15T10:35:12"
    }
  ]
}
```

### Get Media File

Retrieve the actual media file. No authentication required.

**Endpoint**: `GET /posts/{postId}/media/{mediaId}`

**Response**: The actual media file with appropriate content type header

### Upload Media

Upload media for a post. Requires authentication and the user must be the post author.

**Endpoint**: `POST /posts/{postId}/media/upload`

**Headers**:

```
Content-Type: multipart/form-data
Authorization: Basic <base64-encoded-credentials>
```

**Form Data**:

- `file`: The file to upload

**Response**:

```json
{
  "success": true,
  "message": "Media uploaded successfully",
  "data": {
    "id": "68b7a8a25035a56543b201cb",
    "fileName": "beach.jpg",
    "fileUrl": "/posts/68b7a8a25035a56543b201ca/media/68b7a8a25035a56543b201cb",
    "fileType": "image/jpeg",
    "fileSize": 2048576,
    "uploaderId": "68b7a8a25035a56543b201c9",
    "postId": "68b7a8a25035a56543b201ca",
    "uploadedAt": "2023-09-15T10:35:12"
  }
}
```

### Delete Media

Delete a media file. Requires authentication and the user must be the post author.

**Endpoint**: `DELETE /posts/{postId}/media/{mediaId}`

**Headers**:

```
Authorization: Basic <base64-encoded-credentials>
```

**Response**:

```json
{
  "success": true,
  "message": "Media deleted successfully",
  "data": null
}
```

## Frontend Integration Tips

### Working with Pagination

The API returns pagination metadata in all paginated responses:

```json
"pagination": {
  "page": 0,        // Current page (0-based)
  "size": 10,       // Items per page
  "totalElements": 42, // Total number of items
  "totalPages": 5,  // Total number of pages
  "first": true,    // Is this the first page?
  "last": false     // Is this the last page?
}
```

Use this data to build pagination UI components:

```jsx
function Pagination({ pagination, onPageChange }) {
  return (
    <div className="pagination">
      <button
        disabled={pagination.first}
        onClick={() => onPageChange(pagination.page - 1)}
      >
        Previous
      </button>
      <span>
        Page {pagination.page + 1} of {pagination.totalPages}
      </span>
      <button
        disabled={pagination.last}
        onClick={() => onPageChange(pagination.page + 1)}
      >
        Next
      </button>
    </div>
  );
}
```

### Handling Media

1. For displaying images or videos, use the media URL from the post's `mediaUrls` array.
2. The actual URL will be: `http://localhost:8080` + mediaUrl
3. When uploading media:
   - Use `FormData` to send the file
   - Set the proper content type header
   - Handle progress events for large uploads

Example media upload in JavaScript:

```javascript
const uploadMedia = async (postId, file, authToken) => {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch(
      `http://localhost:8080/posts/${postId}/media/upload`,
      {
        method: "POST",
        headers: {
          Authorization: `Basic ${authToken}`,
        },
        body: formData,
      }
    );

    const result = await response.json();

    if (result.success) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error("Error uploading media:", error);
    throw error;
  }
};
```

### Error Handling

Always check the `success` field in API responses to determine if the request was successful:

```javascript
const fetchPosts = async () => {
  try {
    const response = await fetch("http://localhost:8080/posts");
    const result = await response.json();

    if (result.success) {
      // Process the data
      setPosts(result.data);
      setPagination(result.pagination);
    } else {
      // Handle the error based on the message
      showErrorNotification(result.message);
    }
  } catch (error) {
    // Handle network or other errors
    showErrorNotification("Failed to connect to the server");
  }
};
```

### Authentication

Store the authentication token securely and include it in all authenticated requests:

```javascript
// Store token after successful login
localStorage.setItem("authToken", btoa(`${username}:${password}`));

// Add auth header to requests
const authToken = localStorage.getItem("authToken");
const headers = {
  "Content-Type": "application/json",
  Authorization: `Basic ${authToken}`,
};

// Make authenticated request
const response = await fetch("http://localhost:8080/posts", {
  method: "POST",
  headers: headers,
  body: JSON.stringify(postData),
});
```

## React Component Examples

### Post List Component

```jsx
import React, { useState, useEffect } from "react";

function PostList() {
  const [posts, setPosts] = useState([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPosts = async (page = 0) => {
    try {
      setLoading(true);
      const response = await fetch(
        `http://localhost:8080/posts?page=${page}&size=10`
      );
      const result = await response.json();

      if (result.success) {
        setPosts(result.data);
        setPagination(result.pagination);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError("Failed to fetch posts");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts();
  }, []);

  const handlePageChange = (newPage) => {
    fetchPosts(newPage);
  };

  if (loading) return <div>Loading posts...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="post-list">
      <h1>Recent Travel Experiences</h1>

      {posts.length === 0 ? (
        <p>No posts found</p>
      ) : (
        posts.map((post) => (
          <div key={post.id} className="post-card">
            <h2>{post.title}</h2>
            <div className="post-meta">
              <span>
                By {post.author.firstName} {post.author.lastName}
              </span>
              <span>at {new Date(post.createdAt).toLocaleDateString()}</span>
              <span>in {post.location}</span>
            </div>
            {post.mediaUrls.length > 0 && (
              <div className="post-media">
                <img
                  src={`http://localhost:8080${post.mediaUrls[0]}`}
                  alt={post.title}
                />
              </div>
            )}
            <p className="post-content">{post.content}</p>
            <div className="post-tags">
              {post.tags.map((tag) => (
                <span key={tag} className="tag">
                  #{tag}
                </span>
              ))}
            </div>
            <div className="post-stats">
              <span>👍 {post.upvotes}</span>
              <span>👎 {post.downvotes}</span>
              <span>💬 {post.commentCount}</span>
            </div>
          </div>
        ))
      )}

      {/* Pagination component */}
      <div className="pagination">
        <button
          disabled={pagination.first}
          onClick={() => handlePageChange(pagination.page - 1)}
        >
          Previous
        </button>
        <span>
          Page {pagination.page + 1} of {pagination.totalPages || 1}
        </span>
        <button
          disabled={pagination.last}
          onClick={() => handlePageChange(pagination.page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}

export default PostList;
```

### Post Create Form

```jsx
import React, { useState } from "react";

function CreatePostForm() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [location, setLocation] = useState("");
  const [tags, setTags] = useState("");
  const [published, setPublished] = useState(true);
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Get auth token from storage
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to create a post");
        setLoading(false);
        return;
      }

      // Create the post
      const postData = {
        title,
        content,
        location,
        tags: tags.split(",").map((tag) => tag.trim()),
        published,
      };

      const response = await fetch("http://localhost:8080/posts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Basic ${authToken}`,
        },
        body: JSON.stringify(postData),
      });

      const result = await response.json();

      if (result.success) {
        // If there's a file to upload and post was created successfully
        if (file && result.data.id) {
          const formData = new FormData();
          formData.append("file", file);

          const mediaResponse = await fetch(
            `http://localhost:8080/posts/${result.data.id}/media/upload`,
            {
              method: "POST",
              headers: {
                Authorization: `Basic ${authToken}`,
              },
              body: formData,
            }
          );

          const mediaResult = await mediaResponse.json();

          if (!mediaResult.success) {
            console.warn(
              "Post created but media upload failed:",
              mediaResult.message
            );
          }
        }

        // Clear the form
        setTitle("");
        setContent("");
        setLocation("");
        setTags("");
        setFile(null);
        setSuccess(true);

        // Hide success message after 3 seconds
        setTimeout(() => setSuccess(false), 3000);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError("Failed to create post. Please try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="create-post-form">
      <h2>Share Your Travel Experience</h2>

      {error && <div className="error-message">{error}</div>}
      {success && (
        <div className="success-message">Post created successfully!</div>
      )}

      <div className="form-group">
        <label htmlFor="title">Title:</label>
        <input
          type="text"
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="content">Content:</label>
        <textarea
          id="content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          required
          rows={5}
        />
      </div>

      <div className="form-group">
        <label htmlFor="location">Location:</label>
        <input
          type="text"
          id="location"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="tags">Tags (comma-separated):</label>
        <input
          type="text"
          id="tags"
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder="beach, vacation, hiking"
        />
      </div>

      <div className="form-group">
        <label htmlFor="media">Upload Image:</label>
        <input
          type="file"
          id="media"
          accept="image/*"
          onChange={(e) => setFile(e.target.files[0])}
        />
      </div>

      <div className="form-group">
        <label htmlFor="published">
          <input
            type="checkbox"
            id="published"
            checked={published}
            onChange={(e) => setPublished(e.target.checked)}
          />
          Publish immediately
        </label>
      </div>

      <button type="submit" disabled={loading}>
        {loading ? "Creating..." : "Create Post"}
      </button>
    </form>
  );
}

export default CreatePostForm;
```
