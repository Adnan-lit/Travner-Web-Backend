package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.service.MediaService;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for handling media uploads and retrievals from GridFS
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("posts/{postId}/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    /**
     * Get all media for a specific post
     * 
     * @param postId The ID of the post
     * @return List of media associated with the post
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaDTO>>> getMediaForPost(@PathVariable String postId) {
        try {
            List<MediaDTO> media = mediaService.getMediaForPost(postId);
            return ResponseEntity.ok(ApiResponse.success(media));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve media: " + e.getMessage()));
        }
    }

    /**
     * Get a specific media file for download
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<InputStreamResource> getMediaFile(@PathVariable String mediaId) {
        try {
            return mediaService.getMediaFile(mediaId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload a new media file to GridFS
     * 
     * @param authentication User authentication
     * @param postId         The ID of the post
     * @param file           The file to upload
     * @return Media details with access URL
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaDTO>> uploadMedia(
            Authentication authentication,
            @PathVariable String postId,
            @RequestParam("file") MultipartFile file) {

        // Check authentication
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        // Validate file is not empty
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No file provided or file is empty"));
        }

        try {
            // Validate post ID format
            try {
                new ObjectId(postId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Invalid post ID format"));
            }

            // Upload the file
            MediaDTO uploadedMedia = mediaService.uploadMedia(file, authentication.getName(), postId);

            // Return success response
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Media uploaded successfully", uploadedMedia));
        } catch (Exception e) {
            // Log the error
            log.error("Error uploading media: {}", e.getMessage(), e);

            // Return error response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to upload media: " + e.getMessage()));
        }
    }

    /**
     * Delete a media file from GridFS
     * 
     * @param authentication User authentication
     * @param mediaId        The ID of the media to delete
     * @return Success message
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Object>> deleteMedia(
            Authentication authentication,
            @PathVariable String mediaId) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            mediaService.deleteMedia(mediaId, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Media deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete media: " + e.getMessage()));
        }
    }
}
