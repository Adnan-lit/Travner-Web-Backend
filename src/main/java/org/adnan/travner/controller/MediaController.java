package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.service.MediaService;
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
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    /**
     * General media upload endpoint for products, posts, profiles, etc.
     *
     * @param authentication User authentication
     * @param file The file to upload
     * @param type Upload type (product, post, profile, etc.)
     * @param entityId Optional entity ID (productId, postId, etc.)
     * @return Media details with access URL
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaDTO>> uploadMedia(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "general") String type,
            @RequestParam(value = "entityId", required = false) String entityId) {

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
            // Upload the file with type-specific handling
            MediaDTO uploadedMedia = mediaService.uploadMedia(file, authentication.getName(), type, entityId);

            // Return success response
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Media uploaded successfully", uploadedMedia));
        } catch (Exception e) {
            log.error("Error uploading media: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload media: " + e.getMessage()));
        }
    }

    /**
     * Get media file by filename for serving
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<InputStreamResource> getMediaFileByName(@PathVariable String filename) {
        try {
            return mediaService.getMediaFileByName(filename);
        } catch (IOException e) {
            log.error("Error retrieving media file {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get all media for a specific post
     *
     * @param postId The ID of the post
     * @return List of media associated with the post
     */
    @GetMapping("/posts/{postId}")
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
     * Get all media for a specific entity
     *
     * @param entityId The ID of the entity
     * @param type Optional type filter (post, product, profile, etc.)
     * @return List of media associated with the entity
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<ApiResponse<List<MediaDTO>>> getMediaForEntity(
            @PathVariable String entityId,
            @RequestParam(required = false) String type) {
        try {
            List<MediaDTO> media = mediaService.getMediaForEntity(entityId, type);
            return ResponseEntity.ok(ApiResponse.success(media));
        } catch (Exception e) {
            log.error("Error retrieving media for entity {}: {}", entityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve media: " + e.getMessage()));
        }
    }

    /**
     * Get a specific media file by ID for download
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<InputStreamResource> getMediaFile(@PathVariable String mediaId) {
        try {
            return mediaService.getMediaFile(mediaId);
        } catch (IOException e) {
            log.error("Error retrieving media file by ID {}: {}", mediaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
            log.error("Error deleting media {}: {}", mediaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete media: " + e.getMessage()));
        }
    }
}
