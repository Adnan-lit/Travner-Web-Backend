package org.adnan.travner.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.entry.MediaEntry;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.MediaRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.util.FileValidationUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Upload a media file to GridFS with type-specific handling
     */
    @Transactional
    public MediaDTO uploadMedia(MultipartFile file, String username, String type, String entityId) {
        // Validate file using security utility
        FileValidationUtil.validateFile(file);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // Find the user
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Type-specific validation
        if ("post".equals(type) && entityId != null) {
            validatePostOwnership(entityId, user);
        }

        try {
            // Get file metadata and sanitize filename
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = FileValidationUtil.sanitizeFilename(originalFilename);
            if (sanitizedFilename.equals("file")) {
                sanitizedFilename = type + "_" + System.currentTimeMillis();
            }

            // Create unique filename with UUID
            String uniqueFilename = UUID.randomUUID().toString() + "_" + sanitizedFilename;

            // Store metadata for GridFS
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("uploadedBy", username);
            metadata.put("uploadType", type);
            metadata.put("entityId", entityId);
            metadata.put("originalFilename", originalFilename);
            metadata.put("uploadedAt", LocalDateTime.now().toString());

            // Store file in GridFS
            ObjectId gridFsId = gridFsTemplate.store(file.getInputStream(), uniqueFilename, file.getContentType(), metadata);

            // Create MediaEntry record
            MediaEntry mediaEntry = MediaEntry.builder()
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .uploadedBy(user.getId().toString()) // Fix: Convert ObjectId to String
                    .gridFsId(gridFsId.toString()) // Fix: Convert ObjectId to String
                    .type(type) // Fix: Change from uploadType to type
                    .entityId(entityId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            MediaEntry savedMedia = mediaRepository.save(mediaEntry);

            // Return DTO with file URL
            return MediaDTO.builder()
                    .id(savedMedia.getId().toString())
                    .filename(savedMedia.getFilename())
                    .originalFilename(savedMedia.getOriginalFilename())
                    .contentType(savedMedia.getContentType())
                    .size(savedMedia.getSize())
                    .uploadedBy(savedMedia.getUploadedBy())
                    .type(savedMedia.getType()) // Fix: Change from uploadType to type
                    .entityId(savedMedia.getEntityId())
                    .uploadedAt(savedMedia.getUploadedAt())
                    .downloadUrl("/api/media/files/" + savedMedia.getFilename())
                    .gridFsId(savedMedia.getGridFsId())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method for post-specific uploads (backward compatibility)
     */
    @Transactional
    public MediaDTO uploadMedia(MultipartFile file, String username, String postId) {
        return uploadMedia(file, username, "post", postId);
    }

    private void validatePostOwnership(String postId, UserEntry user) {
        ObjectId postObjectId;
        try {
            postObjectId = new ObjectId(postId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid post ID format: " + postId);
        }

        Optional<PostEntry> postOptional = postRepository.findById(postObjectId);
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found with ID: " + postId);
        }

        PostEntry post = postOptional.get();
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to upload media to this post. You must be the post author.");
        }
    }

    /**
     * Get media file by filename for serving
     */
    public ResponseEntity<InputStreamResource> getMediaFileByName(String filename) throws IOException {
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(filename)));

        if (gridFSFile == null) {
            throw new IOException("File not found: " + filename);
        }

        GridFsResource resource = gridFsOperations.getResource(gridFSFile);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename);

        // Fix: Safe access to metadata to prevent NullPointerException
        String contentType = "application/octet-stream"; // default
        if (gridFSFile.getMetadata() != null && gridFSFile.getMetadata().containsKey("_contentType")) {
            contentType = gridFSFile.getMetadata().getString("_contentType");
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(resource.getInputStream()));
    }

    /**
     * Get a list of media files for a post
     */
    public List<MediaDTO> getMediaForPost(String postId) {
        List<MediaEntry> mediaEntries = mediaRepository.findByPostId(new ObjectId(postId));
        return mediaEntries.stream()
                .map(media -> {
                    MediaDTO dto = convertToDTO(media);
                    // Generate URL for accessing the file
                    dto.setDownloadUrl("/posts/" + postId + "/media/" + media.getId()); // Fix: Change from setFileUrl to setDownloadUrl
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a specific media file as a downloadable resource
     */
    public ResponseEntity<InputStreamResource> getMediaFile(String mediaId) throws IOException {
        Optional<MediaEntry> mediaOptional = mediaRepository.findById(new ObjectId(mediaId));
        if (mediaOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MediaEntry media = mediaOptional.get();
        ObjectId gridFsId = new ObjectId(media.getGridFsId());

        // Find the file in GridFS
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(gridFsId)));

        // Get the file resource
        GridFsResource resource = gridFsOperations.getResource(gridFSFile);

        // Set the appropriate content type
        MediaType mediaType = MediaType.parseMediaType(media.getContentType()); // Fix: Change from getFileType to getContentType

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFilename() + "\"") // Fix: Change from getFileName to getFilename
                .body(new InputStreamResource(resource.getInputStream()));
    }

    /**
     * Delete a media file from GridFS and remove its entry
     */
    @Transactional
    public void deleteMedia(String id, String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<MediaEntry> mediaOptional = mediaRepository.findById(new ObjectId(id));
        if (mediaOptional.isEmpty()) {
            throw new RuntimeException("Media not found");
        }

        MediaEntry media = mediaOptional.get();

        // Check if the user is the uploader or an admin
        // Fix: Compare with uploadedBy string field instead of uploader DBRef
        if (!media.getUploadedBy().equals(user.getId().toString()) &&
                (user.getRoles() == null || !user.getRoles().contains("ADMIN"))) {
            throw new RuntimeException("You are not authorized to delete this media");
        }

        // Delete the file from GridFS
        try {
            ObjectId gridFsId = new ObjectId(media.getGridFsId());
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(gridFsId)));
            log.info("Deleted file from GridFS: {}", gridFsId);
        } catch (Exception e) {
            log.error("Failed to delete file from GridFS", e);
            // Continue with deletion from database even if GridFS deletion fails
        }

        // Fix: Only try to update post if postId exists (may be null for non-post media)
        if (media.getPostId() != null) {
            Optional<PostEntry> postOptional = postRepository.findById(media.getPostId());
            if (postOptional.isPresent()) {
                PostEntry post = postOptional.get();
                String mediaUrl = "/posts/" + post.getId() + "/media/" + media.getId();
                if (post.getMediaUrls() != null) {
                    post.getMediaUrls().remove(mediaUrl);
                    postRepository.save(post);
                }
            }
        }

        mediaRepository.delete(media);
    }

    private MediaDTO convertToDTO(MediaEntry media) {
        return MediaDTO.builder()
                .id(media.getId().toString())
                .filename(media.getFilename()) // Fix: Change from fileName to filename
                .contentType(media.getContentType()) // Fix: Change from fileType to contentType
                .size(media.getSize()) // Fix: Change from fileSize to size
                .uploadedBy(media.getUploadedBy()) // Fix: Use uploadedBy instead of uploaderId
                .type(media.getType()) // Add missing field
                .entityId(media.getEntityId()) // Add missing field
                .uploadedAt(media.getUploadedAt())
                .gridFsId(media.getGridFsId())
                .downloadUrl("/api/media/files/" + media.getFilename()) // Add download URL
                .build();
    }
}
