package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for user presence/activity status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresenceDTO {

    public enum Status {
        ONLINE,
        AWAY,
        OFFLINE
    }

    private String userId;
    private String userName;
    private Status status;
    private Instant lastSeen;
    private String currentConversationId; // If user is actively viewing a conversation
}