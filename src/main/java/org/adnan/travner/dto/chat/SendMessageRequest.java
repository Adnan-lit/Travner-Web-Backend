package org.adnan.travner.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.domain.message.Message;

import java.util.List;

/**
 * Request DTO for sending a message in a conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotNull(message = "Message type is required")
    private Message.MessageKind kind;

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    private String content;

    private List<MessageAttachmentRequest> attachments;

    private String replyToMessageId; // For threaded conversations

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageAttachmentRequest {
        @NotBlank(message = "Media ID is required for attachment")
        private String mediaId;

        @Size(max = 200, message = "Caption cannot exceed 200 characters")
        private String caption;
    }
}