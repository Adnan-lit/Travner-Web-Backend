package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.domain.conversation.ConversationMembershipRepository;
import org.adnan.travner.domain.message.Message;
import org.adnan.travner.domain.message.MessageRepository;
import org.adnan.travner.domain.message.MessageReadStatus;
import org.adnan.travner.dto.chat.MessageResponse;
import org.adnan.travner.dto.chat.SendMessageRequest;
import org.adnan.travner.repository.MessageReadStatusRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.util.ObjectIdUtil;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing messages in conversations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationMembershipRepository membershipRepository;
    private final org.adnan.travner.domain.conversation.ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;

    /**
     * Resolve a user identifier that may be either a username or an ObjectId string
     * into the canonical ObjectId of the user document.
     */
    private ObjectId resolveUserId(String usernameOrId) {
        if (org.adnan.travner.util.ObjectIdUtil.isValidObjectId(usernameOrId)) {
            return new ObjectId(usernameOrId);
        }
        var user = userRepository.findByuserName(usernameOrId);
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User not found: " + usernameOrId);
        }
        return user.getId();
    }

    /**
     * Send a message in a conversation
     */
    public MessageResponse sendMessage(SendMessageRequest request, String senderId) {
        log.debug("Sending message in conversation: {} from user: {}", request.getConversationId(), senderId);

        ObjectId conversationObjectId = ObjectIdUtil.safeObjectId(request.getConversationId());
        ObjectId senderObjectId = resolveUserId(senderId);

        // Verify sender is a member of the conversation
        if (!membershipRepository.existsByConversationIdAndUserId(conversationObjectId, senderObjectId)) {
            throw new IllegalArgumentException("User is not a member of this conversation");
        }

        // Create attachments if any
        List<Message.MessageAttachment> attachments = null;
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            attachments = request.getAttachments().stream()
                    .map(attachmentRequest -> Message.MessageAttachment.builder()
                            .id(attachmentRequest.getMediaId())
                            .filename(attachmentRequest.getCaption()) // Using caption as filename for now
                            .url(attachmentRequest.getMediaId()) // Set URL to mediaId for now
                            .contentType("application/octet-stream") // Default content type
                            .size(0) // Size not provided in request
                            .build())
                    .collect(Collectors.toList());
        }

        // Create message entity
        Message message = Message.builder()
                .conversationId(conversationObjectId)
                .senderId(senderObjectId)
                .kind(request.getKind())
                .body(request.getContent())
                .attachments(attachments)
                .replyTo(
                        request.getReplyToMessageId() != null ? ObjectIdUtil.safeObjectId(request.getReplyToMessageId())
                                : null)
                .createdAt(Instant.now())
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Sent message: {} in conversation: {}", savedMessage.getId(), request.getConversationId());

        // Update conversation last activity timestamp
        conversationRepository.findById(conversationObjectId).ifPresent(conv -> {
            conv.setLastMessageAt(Instant.now());
            conversationRepository.save(conv);
        });

        return mapToResponse(savedMessage);
    }

    /**
     * Get messages for a conversation with pagination
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(String conversationId, String userId, Pageable pageable) {
        log.debug("Getting messages for conversation: {} user: {}", conversationId, userId);

        ObjectId conversationObjectId = new ObjectId(conversationId);
        ObjectId userObjectId = resolveUserId(userId);

        // Verify user is a member of the conversation
        if (!membershipRepository.existsByConversationIdAndUserId(conversationObjectId, userObjectId)) {
            throw new IllegalArgumentException("User is not a member of this conversation");
        }

        Page<Message> messages = messageRepository.findByConversationIdAndDeletedAtIsNull(conversationObjectId,
                pageable);

        return messages.map(this::mapToResponse);
    }

    /**
     * Mark messages as read by user
     */
    public void markMessagesAsRead(String conversationId, String lastReadMessageId, String userId) {
        log.debug("Marking messages as read in conversation: {} up to message: {} for user: {}",
                conversationId, lastReadMessageId, userId);

        ObjectId conversationObjectId = new ObjectId(conversationId);
        ObjectId userObjectId = resolveUserId(userId);
        ObjectId lastReadMessageObjectId = new ObjectId(lastReadMessageId);

        // Verify user is a member of the conversation
        if (!membershipRepository.existsByConversationIdAndUserId(conversationObjectId, userObjectId)) {
            throw new IllegalArgumentException("User is not a member of this conversation");
        }

        // Get the timestamp of the last read message
        Message lastReadMessage = messageRepository.findById(lastReadMessageObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Update user's last read timestamp in membership
        membershipRepository.findByConversationIdAndUserId(conversationObjectId, userObjectId)
                .ifPresent(membership -> {
                    membership.setLastReadAt(lastReadMessage.getCreatedAt());
                    membershipRepository.save(membership);
                });

        // Mark individual message as read
        if (messageReadStatusRepository.findByMessageIdAndUserId(lastReadMessageObjectId, userObjectId).isEmpty()) {
            MessageReadStatus readStatus = MessageReadStatus.builder()
                    .messageId(lastReadMessageObjectId)
                    .conversationId(conversationObjectId)
                    .userId(userObjectId)
                    .readAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            messageReadStatusRepository.save(readStatus);
        }

        // Mark all previous messages as read too
        List<Message> unreadMessages = messageRepository.findByConversationIdAndCreatedAtLessThanEqualAndDeletedAtIsNull(
                conversationObjectId, lastReadMessage.getCreatedAt());

        for (Message message : unreadMessages) {
            if (messageReadStatusRepository.findByMessageIdAndUserId(message.getId(), userObjectId).isEmpty()) {
                MessageReadStatus readStatus = MessageReadStatus.builder()
                        .messageId(message.getId())
                        .conversationId(conversationObjectId)
                        .userId(userObjectId)
                        .readAt(Instant.now())
                        .createdAt(Instant.now())
                        .build();
                messageReadStatusRepository.save(readStatus);
            }
        }

        log.info("Marked messages as read for user: {} in conversation: {}", userId, conversationId);
    }

    /**
     * Get unread message count for user in conversation
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String conversationId, String userId) {
        ObjectId conversationObjectId = new ObjectId(conversationId);
        ObjectId userObjectId = resolveUserId(userId);

        // Count total messages in conversation
        long totalMessages = messageRepository.countByConversationIdAndDeletedAtIsNull(conversationObjectId);

        // Count read messages by this user
        long readMessages = messageReadStatusRepository.findByConversationIdAndUserId(conversationObjectId, userObjectId).size();

        // Return unread count
        return Math.max(0, totalMessages - readMessages);
    }

    /**
     * Edit a message
     */
    public MessageResponse editMessage(String messageId, String newContent, String userId) {
        log.debug("Editing message: {} by user: {}", messageId, userId);

        ObjectId messageObjectId = new ObjectId(messageId);
        ObjectId userObjectId = resolveUserId(userId);

        Message message = messageRepository.findById(messageObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Verify the user is the sender
        if (!message.getSenderId().equals(userObjectId)) {
            throw new IllegalArgumentException("Only message sender can edit the message");
        }

        // Update message content
        message.setBody(newContent);
        message.setEditedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);
        log.info("Edited message: {}", messageId);

        return mapToResponse(savedMessage);
    }

    /**
     * Delete a message (soft delete)
     */
    public void deleteMessage(String messageId, String userId) {
        log.debug("Deleting message: {} by user: {}", messageId, userId);

        ObjectId messageObjectId = new ObjectId(messageId);
        ObjectId userObjectId = resolveUserId(userId);

        Message message = messageRepository.findById(messageObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Verify the user is the sender
        if (!message.getSenderId().equals(userObjectId)) {
            throw new IllegalArgumentException("Only message sender can delete the message");
        }

        // Soft delete by setting deletedAt timestamp
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);
        log.info("Deleted message: {}", messageId);
    }

    /**
     * Map message entity to response DTO
     */
    private MessageResponse mapToResponse(Message message) {
        List<MessageResponse.MessageAttachmentResponse> attachmentResponses = null;
        if (message.getAttachments() != null) {
            attachmentResponses = message.getAttachments().stream()
                    .map(attachment -> MessageResponse.MessageAttachmentResponse.builder()
                            .mediaId(attachment.getId())
                            .fileName(attachment.getFilename())
                            .contentType(attachment.getContentType())
                            .fileSize(attachment.getSize())
                            .downloadUrl(attachment.getUrl())
                            .caption(attachment.getFilename()) // Using filename as caption for now
                            .build())
                    .collect(Collectors.toList());
        }

        return MessageResponse.builder()
                .id(message.getId().toString())
                .conversationId(message.getConversationId().toString())
                .senderId(message.getSenderId().toString())
                .senderName(getSenderName(message.getSenderId()))
                .kind(message.getKind())
                .content(message.getBody())
                .attachments(attachmentResponses)
                .replyToMessageId(message.getReplyTo() != null ? message.getReplyTo().toString() : null)
                .sentAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .isEdited(message.getEditedAt() != null)
                .readByUserIds(getMessageReadByUserIds(message))
                .readCount(getMessageReadCount(message))
                .build();
    }

    private String getSenderName(ObjectId senderId) {
        return userRepository.findById(senderId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Unknown User");
    }

    private List<String> getMessageReadByUserIds(Message message) {
        List<MessageReadStatus> readStatuses = messageReadStatusRepository.findByMessageId(message.getId());
        return readStatuses.stream()
                .map(status -> status.getUserId().toString())
                .collect(Collectors.toList());
    }

    private int getMessageReadCount(Message message) {
        return (int) messageReadStatusRepository.findByMessageId(message.getId()).size();
    }
}