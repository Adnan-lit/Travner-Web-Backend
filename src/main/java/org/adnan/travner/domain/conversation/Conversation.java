package org.adnan.travner.domain.conversation;

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
 * Represents a conversation (DIRECT or GROUP) in the chat system
 */
@Document(collection = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private ObjectId id;

    private ConversationType type;

    @Indexed
    private List<ObjectId> memberIds;

    private String title; // For GROUP conversations

    private ObjectId ownerId; // For GROUP conversations

    private List<ObjectId> adminIds; // For GROUP conversations

    private Instant createdAt;

    @Indexed
    private Instant lastMessageAt;

    @Builder.Default
    private boolean isArchived = false;

    public enum ConversationType {
        DIRECT, GROUP
    }
}