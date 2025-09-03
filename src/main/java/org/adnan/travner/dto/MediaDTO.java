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
    private String fileName;
    private String fileUrl; // URL to access the file via the API
    private String fileType;
    private long fileSize;
    private String uploaderId;
    private String postId;
    private LocalDateTime uploadedAt;
}
