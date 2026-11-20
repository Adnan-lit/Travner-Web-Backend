package org.adnan.travner.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.entry.MediaEntry;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.ProductEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.repository.MediaRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.ProductRepository;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ProductRepository productRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Upload a media file to GridFS with type-specific handling
     */
    @Transactional
    public MediaDTO uploadMedia(MultipartFile file, String username, String type, String entityId) {
        // Magic bytes are the source of truth; the detected MIME is stored.
        FileValidationUtil.ValidatedMedia validated = FileValidationUtil.validateFile(file);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Object-level authorization for entity-specific uploads.
        if ("post".equals(type) && entityId != null) {
            validatePostOwnership(entityId, user);
        }
        if ("product".equals(type) && entityId != null) {
            validateProductOwnership(entityId, user, username);
        }

        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = FileValidationUtil.sanitizeFilename(originalFilename);
        if ("file".equals(sanitizedFilename)) {
            sanitizedFilename = type + "_" + System.currentTimeMillis();
        }
        String uniqueFilename = UUID.randomUUID().toString() + "_" + sanitizedFilename;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("uploadedBy", username);
        metadata.put("uploadType", type);
        metadata.put("entityId", entityId);
        metadata.put("originalFilename", originalFilename);
        metadata.put("uploadedAt", LocalDateTime.now().toString());

        ObjectId gridFsId = null;
        MediaEntry savedMedia = null;
        try {
            gridFsId = gridFsTemplate.store(
                    new ByteArrayInputStream(validated.bytes()), uniqueFilename, validated.contentType(), metadata);

            savedMedia = mediaRepository.save(MediaEntry.builder()
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .contentType(validated.contentType())
                    .size((long) validated.bytes().length)
                    .uploadedBy(user.getId().toString())
                    .gridFsId(gridFsId.toString())
                    .type(type)
                    .entityId(entityId)
                    .uploadedAt(LocalDateTime.now())
                    .build());

            // Single, idempotent association point (P2-04). The same URL is stored
            // on the entity and returned to the client.
            String mediaUrl = "/api/media/" + savedMedia.getId();
            if ("post".equals(type) && entityId != null) {
                associateWithPost(savedMedia, entityId, mediaUrl);
            } else if ("product".equals(type) && entityId != null) {
                associateWithProduct(savedMedia, entityId, mediaUrl);
            }

            return MediaDTO.builder()
                    .id(savedMedia.getId().toString())
                    .filename(savedMedia.getFilename())
                    .originalFilename(savedMedia.getOriginalFilename())
                    .contentType(savedMedia.getContentType())
                    .size(savedMedia.getSize())
                    .uploadedBy(savedMedia.getUploadedBy())
                    .type(savedMedia.getType())
                    .entityId(savedMedia.getEntityId())
                    .uploadedAt(savedMedia.getUploadedAt())
                    .downloadUrl(mediaUrl)
                    .gridFsId(savedMedia.getGridFsId())
                    .build();
        } catch (RuntimeException e) {
            // Never leave an unreferenced GridFS object or DB row on a failed association.
            if (gridFsId != null) {
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(gridFsId)));
            }
            if (savedMedia != null && savedMedia.getId() != null) {
                mediaRepository.delete(savedMedia);
            }
            throw e;
        }
    }

    private void associateWithPost(MediaEntry media, String postId, String mediaUrl) {
        ObjectId postObjectId = new ObjectId(postId);
        media.setPostId(postObjectId);
        mediaRepository.save(media);
        postRepository.findById(postObjectId).ifPresent(post -> {
            if (post.getMediaUrls() == null) {
                post.setMediaUrls(new ArrayList<>());
            }
            if (!post.getMediaUrls().contains(mediaUrl)) {
                post.getMediaUrls().add(mediaUrl);
                postRepository.save(post);
            }
        });
    }

    private void associateWithProduct(MediaEntry media, String productId, String mediaUrl) {
        ObjectId productObjectId = new ObjectId(productId);
        media.setProductId(productObjectId);
        mediaRepository.save(media);
        productRepository.findById(productObjectId).ifPresent(product -> {
            if (product.getImages() == null) {
                product.setImages(new ArrayList<>());
            }
            if (!product.getImages().contains(mediaUrl)) {
                product.getImages().add(mediaUrl);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
            }
        });
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
            throw new ForbiddenException("You are not authorized to upload media to this post");
        }
    }

    /** Product media may only be uploaded by the seller or an administrator. */
    private void validateProductOwnership(String productId, UserEntry user, String username) {
        ProductEntry product = productRepository.findById(new ObjectId(productId))
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        boolean isSeller = (product.getSellerId() != null
                && product.getSellerId().equals(user.getId().toHexString()))
                || username.equals(product.getSellerUsername());
        if (!isAdmin && !isSeller) {
            throw new ForbiddenException("Only the product seller or an administrator can upload product media");
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
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + safeDispositionName(filename) + "\"");
        headers.add("X-Content-Type-Options", "nosniff");

        String contentType = "application/octet-stream";
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
                    dto.setDownloadUrl("/api/media/" + media.getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a list of media files for a specific entity
     */
    public List<MediaDTO> getMediaForEntity(String entityId, String type) {
        List<MediaEntry> mediaEntries;
        if (type != null && !type.trim().isEmpty()) {
            mediaEntries = mediaRepository.findByEntityIdAndType(entityId, type);
        } else {
            mediaEntries = mediaRepository.findByEntityId(entityId);
        }
        return mediaEntries.stream()
                .map(media -> {
                    MediaDTO dto = convertToDTO(media);
                    dto.setDownloadUrl("/api/media/" + media.getId());
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
        if (gridFSFile == null) {
            throw new IOException("File not found for media " + mediaId);
        }
        GridFsResource resource = gridFsOperations.getResource(gridFSFile);

        MediaType mediaType = MediaType.parseMediaType(media.getContentType());
        String displayName = media.getOriginalFilename() != null
                ? media.getOriginalFilename() : media.getFilename();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeDispositionName(displayName) + "\"")
                .header("X-Content-Type-Options", "nosniff")
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
            throw new ForbiddenException("You are not authorized to delete this media");
        }

        // Remove the GridFS object first; keep the DB row if this fails so the
        // orphan can be reclaimed by a retry instead of being silently lost.
        ObjectId gridFsId = new ObjectId(media.getGridFsId());
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(gridFsId)));
        log.info("Deleted file from GridFS: {}", gridFsId);

        String mediaUrl = "/api/media/" + media.getId();
        if (media.getPostId() != null) {
            postRepository.findById(media.getPostId()).ifPresent(post -> {
                if (post.getMediaUrls() != null && post.getMediaUrls().remove(mediaUrl)) {
                    postRepository.save(post);
                }
            });
        }
        if (media.getProductId() != null) {
            removeProductImage(media.getProductId(), mediaUrl);
        }

        mediaRepository.delete(media);
    }

    /**
     * Deletes a product image and its GridFS object. The caller must have
     * already authorized the seller/admin relationship to the product.
     * The URL must belong to media associated with this product, which prevents
     * a caller from deleting another product's media by guessing a URL.
     */
    @Transactional
    public void deleteProductMedia(String productId, String mediaUrl) {
        ObjectId productObjectId = new ObjectId(productId);
        MediaEntry target = mediaRepository.findByEntityIdAndType(productId, "product").stream()
                .filter(media -> mediaUrl.equals("/api/media/" + media.getId())
                        || mediaUrl.equals("/api/media/files/" + media.getFilename()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Media not found on this product"));

        gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(target.getGridFsId()))));
        // The product stores the canonical ID URL; accept the legacy filename URL
        // from older clients but always remove the stored canonical form too.
        removeProductImage(productObjectId, "/api/media/" + target.getId());
        removeProductImage(productObjectId, mediaUrl);
        mediaRepository.delete(target);
    }

    private void removeProductImage(ObjectId productId, String mediaUrl) {
        productRepository.findById(productId).ifPresent(product -> {
            if (product.getImages() != null && product.getImages().remove(mediaUrl)) {
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
            }
        });
    }

    /** Prevents CRLF/quote injection into the Content-Disposition header. */
    private static String safeDispositionName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return name.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private MediaDTO convertToDTO(MediaEntry media) {
        return MediaDTO.builder()
                .id(media.getId().toString())
                .filename(media.getFilename()) // Fix: Change from fileName to filename
                .originalFilename(media.getOriginalFilename())
                .contentType(media.getContentType()) // Fix: Change from fileType to contentType
                .size(media.getSize()) // Fix: Change from fileSize to size
                .uploadedBy(media.getUploadedBy()) // Fix: Use uploadedBy instead of uploaderId
                .type(media.getType()) // Add missing field
                .entityId(media.getEntityId()) // Add missing field
                .uploadedAt(media.getUploadedAt())
                .gridFsId(media.getGridFsId())
                .downloadUrl("/api/media/" + media.getId())
                .build();
    }
}