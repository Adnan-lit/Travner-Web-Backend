package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Media files.
 * This represents the metadata about a media file to be sent to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDTO {
    private String id;
    private String filename;        // Fixed: consistent naming
    private String originalFilename;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private String type; // post, profile, product, etc.
    private String entityId; // associated post/product ID
    private LocalDateTime uploadedAt;
    private String downloadUrl;     // Fixed: was fileUrl, now matches usage
    private String gridFsId;
}
