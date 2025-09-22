package org.adnan.travner.domain.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Represents a message in a conversation
 */
@Document(collection = "messages")
@CompoundIndex(def = "{'conversationId': 1, 'createdAt': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private ObjectId id;

    private ObjectId conversationId;

    private ObjectId senderId;

    private String body;

    private MessageKind kind;

    private List<MessageAttachment> attachments;

    private ObjectId replyTo; // Reference to another message

    private Instant createdAt;

    private Instant editedAt;

    private Instant deletedAt; // Soft delete

    public enum MessageKind {
        TEXT, IMAGE, FILE, SYSTEM
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageAttachment {
        private String id;
        private String url;
        private String contentType;
        private long size;
        private String filename;
    }
}