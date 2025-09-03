package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for handling post-related operations
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * Get all published posts with pagination and sorting
     * 
     * @param page      Zero-based page index (default 0)
     * @param size      Page size (default 10)
     * @param sortBy    Field to sort by (default createdAt)
     * @param direction Sort direction: asc or desc (default desc)
     * @return List of published posts with pagination metadata
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostDTO>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

            // Make sure the field to sort by exists
            Pageable pageable;
            try {
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            } catch (Exception e) {
                // If the sort field is invalid, fall back to sorting by createdAt
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));
            }

            Page<PostDTO> posts = postService.getAllPublishedPosts(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve posts: " + e.getMessage()));
        }
    }

    /**
     * Get a specific post by ID
     * 
     * @param id Post ID
     * @return Post details if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable String id) {
        Optional<PostDTO> post = postService.getPostById(id);
        if (post.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(post.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Post not found with ID: " + id));
        }
    }

    /**
     * Get posts by a specific user
     * 
     * @param username Username of the post author
     * @param page     Zero-based page index (default 0)
     * @param size     Page size (default 10)
     * @return List of posts by the user with pagination metadata
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PostDTO> posts = postService.getPostsByUser(username, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve posts by user: " + e.getMessage()));
        }
    }

    /**
     * Search posts by query term
     * 
     * @param query Search term
     * @param page  Zero-based page index (default 0)
     * @param size  Page size (default 10)
     * @return List of matching posts with pagination metadata
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PostDTO>>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PostDTO> posts = postService.searchPosts(query, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search posts: " + e.getMessage()));
        }
    }

    /**
     * Get posts by location
     * 
     * @param location Location name (partial or full)
     * @param page     Zero-based page index (default 0)
     * @param size     Page size (default 10)
     * @return List of posts from the specified location with pagination metadata
     */
    @GetMapping("/location")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByLocation(
            @RequestParam String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PostDTO> posts = postService.getPostsByLocation(location, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get posts by location: " + e.getMessage()));
        }
    }

    /**
     * Get posts by tags
     * 
     * @param tags List of tags to filter by
     * @param page Zero-based page index (default 0)
     * @param size Page size (default 10)
     * @return List of posts with the specified tags with pagination metadata
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PostDTO> posts = postService.getPostsByTags(tags, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get posts by tags: " + e.getMessage()));
        }
    }

    /**
     * Create a new post
     * 
     * @param authentication User authentication
     * @param postRequest    Post data
     * @return Created post details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostDTO>> createPost(
            Authentication authentication,
            @RequestBody PostRequest postRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO createdPost = postService.createPost(authentication.getName(), postRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Post created successfully", createdPost));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create post: " + e.getMessage()));
        }
    }

    /**
     * Update an existing post
     * 
     * @param authentication User authentication
     * @param id             Post ID
     * @param postRequest    Updated post data
     * @return Updated post details
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody PostRequest postRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updatePost(id, authentication.getName(), postRequest);
            return ResponseEntity.ok(ApiResponse.success("Post updated successfully", updatedPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to update post: " + e.getMessage()));
        }
    }

    /**
     * Delete a post
     * 
     * @param authentication User authentication
     * @param id             Post ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePost(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            postService.deletePost(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete post: " + e.getMessage()));
        }
    }

    /**
     * Upvote a post
     * 
     * @param authentication User authentication
     * @param id             Post ID
     * @return Updated post with new upvote count
     */
    @PostMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<PostDTO>> upvotePost(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updateVote(id, authentication.getName(), true);
            return ResponseEntity.ok(ApiResponse.success("Post upvoted successfully", updatedPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to upvote post: " + e.getMessage()));
        }
    }

    /**
     * Downvote a post
     * 
     * @param authentication User authentication
     * @param id             Post ID
     * @return Updated post with new downvote count
     */
    @PostMapping("/{id}/downvote")
    public ResponseEntity<ApiResponse<PostDTO>> downvotePost(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updateVote(id, authentication.getName(), false);
            return ResponseEntity.ok(ApiResponse.success("Post downvoted successfully", updatedPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to downvote post: " + e.getMessage()));
        }
    }
}
