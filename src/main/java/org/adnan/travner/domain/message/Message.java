package org.adnan.travner.domain.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Represents a message in a conversation
 */
@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId conversationId;

    @Indexed
    private ObjectId senderId;

    private String senderUsername;

    private MessageKind kind;

    private String content;

    private List<Attachment> attachments;

    private ObjectId replyToMessageId;

    private Instant createdAt;

    @Indexed
    private Instant updatedAt;

    private boolean isEdited;

    private Instant deletedAt;

    private List<ReadReceipt> readBy;

    public enum MessageKind {
        TEXT, IMAGE, FILE, SYSTEM
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attachment {
        private String mediaId;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String downloadUrl;
        private String caption;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadReceipt {
        private ObjectId userId;
        private String username;
        private Instant readAt;
    }
}