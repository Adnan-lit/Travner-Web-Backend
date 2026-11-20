package org.adnan.travner.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.chat.ChatAttachmentDTO;
import org.adnan.travner.entry.ChatAttachment;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.repository.ChatAttachmentRepository;
import org.adnan.travner.repository.ChatConversationRepository;
import org.adnan.travner.util.ChatFileValidator;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stores and serves chat attachments in GridFS with per-conversation authorization. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAttachmentService {

    private final GridFsTemplate gridFsTemplate;
    private final ChatAttachmentRepository attachmentRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatService chatService;

    /** Streamed file plus the headers needed to serve it safely. */
    public record Download(String filename, String contentType, long size, InputStreamResource resource) {
    }

    public ChatAttachmentDTO upload(MultipartFile file, String conversationId, String userId) {
        // Membership is checked before any bytes are read or stored.
        chatService.assertParticipant(conversationId, userId);

        byte[] bytes = ChatFileValidator.validate(file);
        String contentType = ChatFileValidator.contentTypeOf(bytes);
        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_"
                + ChatFileValidator.sanitizeFilename(originalFilename);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId);
        metadata.put("uploaderId", userId);
        metadata.put("originalFilename", originalFilename);
        metadata.put("uploadedAt", LocalDateTime.now().toString());

        ObjectId gridFsId = gridFsTemplate.store(
                new ByteArrayInputStream(bytes), storedFilename, contentType, metadata);

        try {
            ChatAttachment attachment = ChatAttachment.builder()
                    .conversationId(conversationId)
                    .uploaderId(userId)
                    .gridFsId(gridFsId.toString())
                    .storedFilename(storedFilename)
                    .originalFilename(ChatFileValidator.sanitizeFilename(originalFilename))
                    .contentType(contentType)
                    .size(bytes.length)
                    .uploadedAt(LocalDateTime.now())
                    .build();
            return ChatAttachmentDTO.from(attachmentRepository.save(attachment));
        } catch (RuntimeException e) {
            // Avoid leaving an unreferenced GridFS object behind.
            deleteObject(gridFsId);
            throw e;
        }
    }

    public Download download(String attachmentId, String userId) {
        ChatAttachment attachment = requireAttachment(attachmentId);
        chatService.assertParticipant(attachment.getConversationId(), userId);

        ObjectId objectId = objectId(attachment);
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(objectId)));
        if (file == null) {
            throw new ResourceNotFoundException("Attachment not found");
        }
        try {
            GridFsResource resource = gridFsTemplate.getResource(file);
            return new Download(attachment.getOriginalFilename(), attachment.getContentType(),
                    attachment.getSize(), new InputStreamResource(resource.getInputStream()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read attachment", e);
        }
    }

    /** Only the uploader (or an admin, checked by the caller) may remove an attachment. */
    public void delete(String attachmentId, String userId) {
        ChatAttachment attachment = requireAttachment(attachmentId);
        chatService.assertParticipant(attachment.getConversationId(), userId);
        if (!userId.equals(attachment.getUploaderId())) {
            throw new ForbiddenException("Only the uploader can delete this attachment");
        }
        deleteObject(objectId(attachment));
        attachmentRepository.delete(attachment);
    }

    /**
     * Removes attachments whose conversation no longer exists. Kept as an
     * explicit maintenance operation so orphaned GridFS objects are reclaimable.
     */
    public int deleteOrphans() {
        int removed = 0;
        for (ChatAttachment attachment : attachmentRepository.findAll()) {
            if (!conversationRepository.existsById(attachment.getConversationId())) {
                deleteObject(objectId(attachment));
                attachmentRepository.delete(attachment);
                removed++;
            }
        }
        return removed;
    }

    public ChatAttachmentDTO getAttachment(String attachmentId, String userId) {
        ChatAttachment attachment = requireAttachment(attachmentId);
        chatService.assertParticipant(attachment.getConversationId(), userId);
        return ChatAttachmentDTO.from(attachment);
    }

    private ChatAttachment requireAttachment(String attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private ObjectId objectId(ChatAttachment attachment) {
        try {
            return new ObjectId(attachment.getGridFsId());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Attachment has an invalid storage reference", e);
        }
    }

    private void deleteObject(ObjectId objectId) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(objectId)));
    }
}
