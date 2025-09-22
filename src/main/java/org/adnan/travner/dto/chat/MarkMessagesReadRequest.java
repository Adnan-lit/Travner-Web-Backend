package org.adnan.travner.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for marking messages as read
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkMessagesReadRequest {

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotBlank(message = "Message ID is required")
    private String lastReadMessageId;
}