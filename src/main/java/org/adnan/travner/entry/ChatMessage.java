package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat message entity for MongoDB
 */
@Document(collection = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
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