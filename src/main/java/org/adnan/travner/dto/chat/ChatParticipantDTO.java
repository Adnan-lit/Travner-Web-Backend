package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Display-safe summary of a chat participant. Carries the stable user ID
 * alongside resolved display data so clients never have to present a raw
 * ObjectId as a username (P2-02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDTO {
    private String id;
    private String username;
    private String displayName;
    private String avatarUrl;
}
