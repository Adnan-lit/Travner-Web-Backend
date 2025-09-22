package org.adnan.travner.domain.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a user's membership in a conversation with role and state
 * information
 */
@Document(collection = "conversation_membership")
@CompoundIndex(def = "{'conversationId': 1, 'userId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMembership {

    @Id
    private ObjectId id;

    private ObjectId conversationId;

    private ObjectId userId;

    private MemberRole role;

    private Instant lastReadAt;

    @Builder.Default
    private boolean muted = false;

    private Instant joinedAt;

    public enum MemberRole {
        OWNER, ADMIN, MEMBER
    }
}