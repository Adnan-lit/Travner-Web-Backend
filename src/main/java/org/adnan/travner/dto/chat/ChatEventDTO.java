package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTO for WebSocket real-time events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEventDTO {

    public enum EventType {
        MESSAGE_SENT,
        MESSAGE_EDITED,
        MESSAGE_DELETED,
        USER_TYPING,
        USER_STOPPED_TYPING,
        USER_JOINED_CONVERSATION,
        USER_LEFT_CONVERSATION,
        CONVERSATION_CREATED,
        CONVERSATION_UPDATED,
        USER_ONLINE,
        USER_OFFLINE,
        MESSAGE_READ
    }

    private EventType type;
    private String conversationId;
    private String userId;
    private String userName;
    private Object data; // MessageResponse, ConversationResponse, or other event-specific data
    private Instant timestamp;
}