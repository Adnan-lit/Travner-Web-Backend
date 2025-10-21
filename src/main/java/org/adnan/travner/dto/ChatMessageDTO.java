package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    @Id
    private String id;
    private String conversationId;
    private String senderId;
    private String senderUsername;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isEdited;
    private String replyToMessageId;
    private List<String> readBy; // List of user IDs who read this message
    private String status; // SENT, DELIVERED, READ
}