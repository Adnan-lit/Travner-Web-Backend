package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a media file stored in MongoDB GridFS.
 * This entity stores metadata about the file, while the actual file content
 * is stored in GridFS.
 */
@Document(collection = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaEntry {

    @Id
    private ObjectId id;

    private String filename;        // Fixed: was fileName, now matches MediaService usage
    private String originalFilename; // Added: missing field
    private String gridFsId;        // ID of the file in GridFS
    private String contentType;     // Added: was fileType, now matches MediaService usage
    private Long size;              // Added: was fileSize, now matches MediaService usage
    private String uploadedBy;      // Added: username of uploader
    private String type;            // Added: media type (post, profile, etc.)
    private String entityId;        // Added: associated entity ID

    @DBRef
    private UserEntry uploader;     // Keep for reference

    private ObjectId postId;        // The post this media belongs to
    private ObjectId productId;     // The product this media belongs to

    @CreatedDate
    private LocalDateTime uploadedAt;
}
