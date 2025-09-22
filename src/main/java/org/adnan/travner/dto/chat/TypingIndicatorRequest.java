package org.adnan.travner.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for typing indicator events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingIndicatorRequest {

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    private boolean isTyping;
}