package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.domain.message.Message;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for message information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private Message.MessageKind kind;
    private String content;
    private List<MessageAttachmentResponse> attachments;
    private String replyToMessageId;
    private MessageResponse replyToMessage; // For displaying the original message in replies
    private Instant sentAt;
    private Instant editedAt;
    private boolean isEdited;
    private List<String> readByUserIds;
    private int readCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageAttachmentResponse {
        private String mediaId;
        private String fileName;
        private String contentType;
        private long fileSize;
        private String downloadUrl;
        private String caption;
    }
}