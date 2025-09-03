# Travner API - Postman Collection Guide

This document provides detailed instructions for setting up and using the Postman collection to test the Travner Social Media API.

## Importing the Collection

1. Open Postman
2. Click on "Import" button at the top left
3. Select "Link" tab and paste this URL: `https://www.postman.com/travner/collection/travner-social-api`
   (Note: This is a placeholder URL. Replace with your actual published collection URL)
4. Click "Import"

## Setting up Environment Variables

For easier testing, create a Postman environment with these variables:

1. `base_url`: Your API base URL (e.g., `http://localhost:8080`)
2. `username`: Your test user's username
3. `password`: Your test user's password
4. `post_id`: ID of a test post (you'll set this after creating your first post)
5. `comment_id`: ID of a test comment (you'll set this after creating your first comment)

## Authentication

The collection uses Basic Authentication. Each request that requires authentication will use the `username` and `password` variables.

## Testing Workflow

Below is a recommended testing workflow to validate all aspects of the API:

### 1. User Registration and Login (if needed)

1. **Register User**

   - Use the existing PublicController endpoint: `POST {{base_url}}/public/create-user`
   - Body:
     ```json
     {
       "userName": "traveler1",
       "firstName": "Travel",
       "lastName": "Explorer",
       "email": "traveler@example.com",
       "password": "password123",
       "roles": ["ROLE_USER"]
     }
     ```

2. **Test Login**
   - Access any authenticated endpoint to verify credentials

### 2. Post Management

1. **Create Post**

   - Request: `POST {{base_url}}/posts`
   - Body:
     ```json
     {
       "title": "My Journey Through the Alps",
       "content": "I spent two amazing weeks hiking through the Swiss Alps...",
       "location": "Swiss Alps, Switzerland",
       "tags": ["hiking", "mountains", "adventure", "europe"],
       "isPublished": true
     }
     ```
   - After successful creation, copy the returned post ID to your `post_id` environment variable

2. **Get Post**

   - Request: `GET {{base_url}}/posts/{{post_id}}`
   - Verify the post details match what you created

3. **Search Posts**

   - Request: `GET {{base_url}}/posts/search?query=Alps`
   - Verify your post appears in the results

4. **Update Post**
   - Request: `PUT {{base_url}}/posts/{{post_id}}`
   - Body:
     ```json
     {
       "title": "My Journey Through the Swiss Alps",
       "content": "I spent two amazing weeks hiking through the Swiss Alps and it was unforgettable...",
       "location": "Swiss Alps, Switzerland",
       "tags": ["hiking", "mountains", "adventure", "europe", "switzerland"],
       "isPublished": true
     }
     ```

### 3. Media Upload

1. **Upload Image**

   - Request: `POST {{base_url}}/posts/{{post_id}}/media/upload`
   - Body (form-data):
     - Key: `file`
     - Value: [select an image file]
     - Type: `File`

2. **Get Media**
   - Request: `GET {{base_url}}/posts/{{post_id}}/media`
   - Verify your uploaded media appears in the list

### 4. Comment System

1. **Add Comment**

   - Request: `POST {{base_url}}/posts/{{post_id}}/comments`
   - Body:
     ```json
     {
       "content": "This looks amazing! How difficult was the hike?"
     }
     ```
   - After successful creation, copy the returned comment ID to your `comment_id` environment variable

2. **Reply to Comment**

   - Request: `POST {{base_url}}/posts/{{post_id}}/comments`
   - Body:
     ```json
     {
       "content": "I was there last summer, the views are breathtaking!",
       "parentCommentId": "{{comment_id}}"
     }
     ```

3. **Get Comments**
   - Request: `GET {{base_url}}/posts/{{post_id}}/comments`
   - Verify your comments appear in the list

### 5. Voting

1. **Upvote Post**

   - Request: `POST {{base_url}}/posts/{{post_id}}/upvote`

2. **Downvote Comment**
   - Request: `POST {{base_url}}/posts/{{post_id}}/comments/{{comment_id}}/downvote`

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

## Advanced Testing Scenarios

1. **Pagination**: Test with different page and size parameters

   - `GET {{base_url}}/posts?page=0&size=5`

2. **Multiple Tag Filtering**:

   - `GET {{base_url}}/posts/tags?tags=mountains&tags=hiking`

3. **Location Search**:
   - `GET {{base_url}}/posts/location?location=Switzerland`
