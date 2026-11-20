package org.adnan.travner.controller;

import org.adnan.travner.exception.ExceptionResponses;
import lombok.RequiredArgsConstructor;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.util.RequestValidation;
import org.adnan.travner.web.ValidObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for handling post-related operations
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "location", "category",
            "published", "upvotes", "downvotes", "commentCount");

    private final PostService postService;
    private final MediaService mediaService;

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
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = RequestValidation.pageable(page, size, sortBy, direction, SORT_FIELDS, "createdAt");

        try {
            Page<PostDTO> posts = postService.getAllPublishedPosts(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get a specific post by ID
     * 
     * @param id Post ID
     * @return Post details if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@ValidObjectId("id") String id) {
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
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<PostDTO> posts = postService.getPostsByUser(username, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<PostDTO> posts = postService.searchPosts(query, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
    @GetMapping("/location/{location}")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByLocation(
            @PathVariable String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<PostDTO> posts = postService.getPostsByLocation(location, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = RequestValidation.pageable(page, size);

        try {
            Page<PostDTO> posts = postService.getPostsByTags(tags, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Create a new post with optional media files
     * 
     * @param authentication User authentication
     * @param postRequest    Post data
     * @return Created post details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostDTO>> createPost(
            Authentication authentication,
            @Valid @RequestBody PostRequest postRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO createdPost = postService.createPost(authentication.getName(), postRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Post created successfully", createdPost));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
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
            @ValidObjectId("id") String id,
            @Valid @RequestBody PostRequest postRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updatePost(id, authentication.getName(), postRequest);
            return ResponseEntity.ok(ApiResponse.success("Post updated successfully", updatedPost));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
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
            @ValidObjectId("id") String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            postService.deletePost(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Upload media files directly associated with a post
     * 
     * @param authentication User authentication
     * @param postId         Post ID
     * @param files          Media files to upload
     * @return List of uploaded media details
     */
    @PostMapping("/{postId}/media")
    public ResponseEntity<ApiResponse<List<MediaDTO>>> uploadPostMedia(
            Authentication authentication,
            @ValidObjectId("postId") String postId,
            @RequestParam("files") MultipartFile[] files) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No files provided"));
        }

        try {
            // Upload each file and collect results
            List<MediaDTO> uploadedMedia = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    MediaDTO media = mediaService.uploadMedia(file, authentication.getName(), "post", postId);
                    uploadedMedia.add(media);
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Media uploaded successfully", uploadedMedia));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Get all media associated with a post
     * 
     * @param postId Post ID
     * @return List of media associated with the post
     */
    @GetMapping("/{postId}/media")
    public ResponseEntity<ApiResponse<List<MediaDTO>>> getPostMedia(@ValidObjectId("postId") String postId) {
        try {
            List<MediaDTO> media = mediaService.getMediaForPost(postId);
            return ResponseEntity.ok(ApiResponse.success(media));
        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Vote on a post
     *
     * @param authentication User authentication
     * @param id             Post ID
     * @param voteRequest    Vote request with isUpvote flag
     * @return Updated post with new vote count
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<PostDTO>> voteOnPost(
            Authentication authentication,
            @ValidObjectId("id") String id,
            @RequestBody Map<String, Boolean> voteRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            Boolean isUpvote = voteRequest.get("isUpvote");
            if (isUpvote == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("isUpvote field is required"));
            }

            PostDTO updatedPost = postService.updateVote(id, authentication.getName(), isUpvote);
            String message = isUpvote ? "Post upvoted successfully" : "Post downvoted successfully";
            return ResponseEntity.ok(ApiResponse.success(message, updatedPost));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
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
            @ValidObjectId("id") String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updateVote(id, authentication.getName(), true);
            return ResponseEntity.ok(ApiResponse.success("Post upvoted successfully", updatedPost));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
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
            @ValidObjectId("id") String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            PostDTO updatedPost = postService.updateVote(id, authentication.getName(), false);
            return ResponseEntity.ok(ApiResponse.success("Post downvoted successfully", updatedPost));
        } catch (RuntimeException e) {
            return ExceptionResponses.from(e);
        }
    }
}