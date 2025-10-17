package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.CommentDTO;
import org.adnan.travner.dto.CommentRequest;
import org.adnan.travner.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping({"/api/posts/{postId}/comments", "/api/comments/posts/{postId}"})
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommentDTO>>> getComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<CommentDTO> comments = commentService.getPostComments(postId, pageable);
            return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve comments: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDTO>> getCommentById(@PathVariable String id) {
        try {
            Optional<CommentDTO> comment = commentService.getCommentById(id);
            return comment.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Comment not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve comment: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            Authentication authentication,
            @PathVariable String postId,
            @RequestBody CommentRequest commentRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CommentDTO createdComment = commentService.createComment(postId, authentication.getName(), commentRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Comment created successfully", createdComment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create comment: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDTO>> updateComment(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody CommentRequest commentRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CommentDTO updatedComment = commentService.updateComment(id, authentication.getName(), commentRequest);
            return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", updatedComment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to update comment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteComment(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            commentService.deleteComment(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete comment: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<CommentDTO>> upvoteComment(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CommentDTO updatedComment = commentService.updateVote(id, authentication.getName(), true);
            return ResponseEntity.ok(ApiResponse.success("Comment upvoted successfully", updatedComment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to upvote comment: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/downvote")
    public ResponseEntity<ApiResponse<CommentDTO>> downvoteComment(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CommentDTO updatedComment = commentService.updateVote(id, authentication.getName(), false);
            return ResponseEntity.ok(ApiResponse.success("Comment downvoted successfully", updatedComment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to downvote comment: " + e.getMessage()));
        }
    }
}
