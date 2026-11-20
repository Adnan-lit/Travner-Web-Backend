package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.entry.ChatAttachment;

import java.time.LocalDateTime;

/** Display/download metadata for a chat attachment. Never contains raw storage paths. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentDTO {

    private String id;
    private String conversationId;
    private String fileName;
    private String contentType;
    private long size;
    /** Authorized download endpoint; the browser must send the Bearer token. */
    private String downloadUrl;
    /** Legacy alias kept for older clients. */
    private String fileUrl;
    private LocalDateTime uploadedAt;

    public static ChatAttachmentDTO from(ChatAttachment attachment) {
        return ChatAttachmentDTO.builder()
                .id(attachment.getId())
                .conversationId(attachment.getConversationId())
                .fileName(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .size(attachment.getSize())
                .downloadUrl("/api/chat/attachments/" + attachment.getId() + "/download")
                .fileUrl("/api/chat/attachments/" + attachment.getId() + "/download")
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
