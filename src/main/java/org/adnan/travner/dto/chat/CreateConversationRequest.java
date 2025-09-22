package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.domain.conversation.Conversation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for creating a new conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationRequest {

    @NotNull(message = "Conversation type is required")
    private Conversation.ConversationType type;

    @NotNull(message = "Member IDs are required")
    @Size(min = 1, max = 50, message = "Conversation must have between 1 and 50 members")
    private List<String> memberIds;

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title; // Required for GROUP conversations
}