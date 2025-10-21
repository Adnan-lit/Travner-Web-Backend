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
 * Chat conversation entity for MongoDB
 */
@Document(collection = "chat_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {
    
    @Id
    private String id;
    
    private List<String> participantIds;
    
    private String title;
    
    private String type; // DIRECT, GROUP
    
    private String lastMessage;
    
    private LocalDateTime lastMessageAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private int unreadCount;
    
    private boolean isActive;
}