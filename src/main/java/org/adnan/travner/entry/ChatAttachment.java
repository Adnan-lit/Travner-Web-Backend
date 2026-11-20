package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** A file uploaded into a conversation, backed by a GridFS object. */
@Document(collection = "chat_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachment {

    @Id
    private String id;

    private String conversationId;

    /** Stable ID of the uploader (JWT subject). */
    private String uploaderId;

    /** GridFS ObjectId hex string. */
    private String gridFsId;

    /** Sanitized, UUID-prefixed name used inside GridFS. */
    private String storedFilename;

    /** Original filename, safe for display. */
    private String originalFilename;

    private String contentType;

    private long size;

    private LocalDateTime uploadedAt;
}
