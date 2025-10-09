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

/**
 * Tracks which users have read which messages
 */
@Document(collection = "message_read_status")
@CompoundIndex(def = "{'messageId': 1, 'userId': 1}", unique = true)
@CompoundIndex(def = "{'conversationId': 1, 'userId': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReadStatus {

    @Id
    private ObjectId id;

    private ObjectId messageId;

    private ObjectId conversationId;

    private ObjectId userId;

    private Instant readAt;

    private Instant createdAt;
}