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
     * Upload a media file to GridFS and create a MediaEntry for it
     */
    @Transactional
    public MediaDTO uploadMedia(MultipartFile file, String username, String postId) {
        // Validate file using security utility
        FileValidationUtil.validateFile(file);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (postId == null || postId.trim().isEmpty()) {
            throw new IllegalArgumentException("Post ID cannot be null or empty");
        }

        // Validate ObjectId format for postId
        ObjectId postObjectId;
        try {
            postObjectId = new ObjectId(postId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid post ID format: " + postId);
        }

        // Find the user
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Find the post
        Optional<PostEntry> postOptional = postRepository.findById(postObjectId);
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found with ID: " + postId);
        }

        PostEntry post = postOptional.get();

        // Check if the user is the author of the post
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not authorized to upload media to this post. You must be the post author.");
        }

        try {
            // Get file metadata and sanitize filename
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = FileValidationUtil.sanitizeFilename(originalFilename);
            if (sanitizedFilename.equals("file")) {
                sanitizedFilename = "media_" + System.currentTimeMillis();
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Log file information
            log.info("Processing file upload: name={}, type={}, size={}",
                    originalFilename, contentType, file.getSize());

            // Store metadata for the file
            Map<String, String> metadata = new HashMap<>();
            metadata.put("postId", postId);
            metadata.put("uploader", username);
            metadata.put("uploadTime", LocalDateTime.now().toString());

            // Store file in GridFS
            ObjectId gridFsId = gridFsTemplate.store(
                    file.getInputStream(),
                    originalFilename,
                    contentType,
                    metadata);

            log.info("File stored in GridFS with ID: {}", gridFsId);

            // Create media entry
            MediaEntry media = MediaEntry.builder()
                    .fileName(originalFilename)
                    .gridFsId(gridFsId.toString())
                    .fileType(contentType)
                    .fileSize(file.getSize())
                    .uploader(user)
                    .postId(postObjectId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            MediaEntry savedMedia = mediaRepository.save(media);
            log.info("Media entry saved with ID: {}", savedMedia.getId());

            // Add the media reference URL to the post's mediaUrls
            String mediaUrl = "/posts/" + postId + "/media/" + savedMedia.getId();
            if (post.getMediaUrls() == null) {
                post.setMediaUrls(new java.util.ArrayList<>());
            }
            post.getMediaUrls().add(mediaUrl);
            postRepository.save(post);
            log.info("Post {} updated with new media URL: {}", postId, mediaUrl);

            // Create DTO with URL for accessing the file
            MediaDTO mediaDTO = convertToDTO(savedMedia);
            mediaDTO.setFileUrl(mediaUrl);

            return mediaDTO;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during file upload: " + e.getMessage());
        }
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
                    dto.setFileUrl("/posts/" + postId + "/media/" + media.getId());
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
        MediaType mediaType = MediaType.parseMediaType(media.getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFileName() + "\"")
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
        if (!media.getUploader().getId().equals(user.getId()) &&
                (user.getRoles() == null || !user.getRoles().contains("ROLE_ADMIN"))) {
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

        // Remove the media URL from the post's mediaUrls
        Optional<PostEntry> postOptional = postRepository.findById(media.getPostId());
        if (postOptional.isPresent()) {
            PostEntry post = postOptional.get();
            String mediaUrl = "/posts/" + post.getId() + "/media/" + media.getId();
            post.getMediaUrls().remove(mediaUrl);
            postRepository.save(post);
        }

        mediaRepository.delete(media);
    }

    private MediaDTO convertToDTO(MediaEntry media) {
        return MediaDTO.builder()
                .id(media.getId().toString())
                .fileName(media.getFileName())
                .fileType(media.getFileType())
                .fileSize(media.getFileSize())
                .uploaderId(media.getUploader().getId().toString())
                .postId(media.getPostId().toString())
                .uploadedAt(media.getUploadedAt())
                .build();
    }
}
