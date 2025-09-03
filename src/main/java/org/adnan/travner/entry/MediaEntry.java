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

    private String fileName;

    private String gridFsId; // ID of the file in GridFS

    private String fileType; // image/jpeg, video/mp4, etc.

    private long fileSize;

    @DBRef
    private UserEntry uploader;

    private ObjectId postId; // The post this media belongs to

    @CreatedDate
    private LocalDateTime uploadedAt;
}
