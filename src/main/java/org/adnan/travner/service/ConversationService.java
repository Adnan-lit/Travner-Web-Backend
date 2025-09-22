package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.domain.conversation.Conversation;
import org.adnan.travner.domain.conversation.ConversationMembership;
import org.adnan.travner.domain.conversation.ConversationMembershipRepository;
import org.adnan.travner.domain.conversation.ConversationRepository;
import org.adnan.travner.domain.message.MessageRepository;
import org.adnan.travner.dto.chat.ConversationResponse;
import org.adnan.travner.dto.chat.CreateConversationRequest;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing conversations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationService {

        private final ConversationRepository conversationRepository;
        private final ConversationMembershipRepository membershipRepository;
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;

        /**
         * Create a new conversation
         */
        public ConversationResponse createConversation(CreateConversationRequest request, String currentUserId) {
                log.debug("Creating conversation for user: {}", currentUserId);

                ObjectId currentUserObjectId = new ObjectId(currentUserId);

                // Create conversation entity
                Conversation conversation = Conversation.builder()
                                .type(request.getType())
                                .title(request.getTitle())
                                .ownerId(currentUserObjectId)
                                .adminIds(List.of(currentUserObjectId))
                                .createdAt(Instant.now())
                                .lastMessageAt(Instant.now())
                                .isArchived(false)
                                .build();

                // Save conversation
                Conversation savedConversation = conversationRepository.save(conversation);

                // Create membership for creator
                ConversationMembership creatorMembership = ConversationMembership.builder()
                                .conversationId(savedConversation.getId())
                                .userId(currentUserObjectId)
                                .role(ConversationMembership.MemberRole.ADMIN)
                                .joinedAt(Instant.now())
                                .lastReadAt(Instant.now())
                                .muted(false)
                                .build();

                membershipRepository.save(creatorMembership);

                // Add other members if specified
                if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
                        List<ConversationMembership> memberships = request.getMemberIds().stream()
                                        .filter(userId -> !userId.equals(currentUserId)) // Don't add creator twice
                                        .map(userId -> ConversationMembership.builder()
                                                        .conversationId(savedConversation.getId())
                                                        .userId(new ObjectId(userId))
                                                        .role(ConversationMembership.MemberRole.MEMBER)
                                                        .joinedAt(Instant.now())
                                                        .lastReadAt(Instant.now())
                                                        .muted(false)
                                                        .build())
                                        .collect(Collectors.toList());

                        membershipRepository.saveAll(memberships);
                }

                log.info("Created conversation: {} with {} members", savedConversation.getId(),
                                1 + (request.getMemberIds() != null ? request.getMemberIds().size() : 0));

                return mapToResponse(savedConversation, List.of(creatorMembership), currentUserId);
        }

        /**
         * Get user's conversations with pagination
         */
        @Transactional(readOnly = true)
        public Page<ConversationResponse> getUserConversations(String userId, Pageable pageable) {
                log.debug("Getting conversations for user: {}", userId);

                ObjectId userObjectId = new ObjectId(userId);
                Page<Conversation> conversations = conversationRepository
                                .findByMemberIdsContainingAndIsArchivedFalse(userObjectId, pageable);

                return conversations.map(conversation -> {
                        List<ConversationMembership> memberships = membershipRepository
                                        .findByConversationId(conversation.getId());
                        return mapToResponse(conversation, memberships, userId);
                });
        }

        /**
         * Get conversation by ID with member check
         */
        @Transactional(readOnly = true)
        public ConversationResponse getConversation(String conversationId, String userId) {
                log.debug("Getting conversation: {} for user: {}", conversationId, userId);

                ObjectId conversationObjectId = new ObjectId(conversationId);
                ObjectId userObjectId = new ObjectId(userId);

                // Check if user is a member
                if (!membershipRepository.existsByConversationIdAndUserId(conversationObjectId, userObjectId)) {
                        throw new IllegalArgumentException("User is not a member of this conversation");
                }

                Conversation conversation = conversationRepository.findById(conversationObjectId)
                                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

                List<ConversationMembership> memberships = membershipRepository
                                .findByConversationId(conversationObjectId);

                return mapToResponse(conversation, memberships, userId);
        }

        /**
         * Add members to conversation
         */
        public void addMembers(String conversationId, List<String> userIds, String currentUserId) {
                log.debug("Adding {} members to conversation: {}", userIds.size(), conversationId);

                ObjectId conversationObjectId = new ObjectId(conversationId);
                ObjectId currentUserObjectId = new ObjectId(currentUserId);

                // Verify current user is admin
                ConversationMembership currentUserMembership = membershipRepository
                                .findByConversationIdAndUserId(conversationObjectId, currentUserObjectId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User is not a member of this conversation"));

                if (currentUserMembership.getRole() != ConversationMembership.MemberRole.ADMIN) {
                        throw new IllegalArgumentException("Only admins can add members");
                }

                // Check which users are not already members
                List<ObjectId> existingMemberIds = membershipRepository
                                .findByConversationId(conversationObjectId)
                                .stream()
                                .map(ConversationMembership::getUserId)
                                .collect(Collectors.toList());

                List<ObjectId> newMemberObjectIds = userIds.stream()
                                .map(ObjectId::new)
                                .filter(userId -> !existingMemberIds.contains(userId))
                                .collect(Collectors.toList());

                if (newMemberObjectIds.isEmpty()) {
                        log.warn("No new members to add to conversation: {}", conversationId);
                        return;
                }

                // Create memberships for new members
                List<ConversationMembership> newMemberships = newMemberObjectIds.stream()
                                .map(userId -> ConversationMembership.builder()
                                                .conversationId(conversationObjectId)
                                                .userId(userId)
                                                .role(ConversationMembership.MemberRole.MEMBER)
                                                .joinedAt(Instant.now())
                                                .lastReadAt(Instant.now())
                                                .muted(false)
                                                .build())
                                .collect(Collectors.toList());

                membershipRepository.saveAll(newMemberships);
                log.info("Added {} new members to conversation: {}", newMemberships.size(), conversationId);
        }

        /**
         * Remove member from conversation
         */
        public void removeMember(String conversationId, String userIdToRemove, String currentUserId) {
                log.debug("Removing member: {} from conversation: {}", userIdToRemove, conversationId);

                ObjectId conversationObjectId = new ObjectId(conversationId);
                ObjectId currentUserObjectId = new ObjectId(currentUserId);
                ObjectId userToRemoveObjectId = new ObjectId(userIdToRemove);

                // Verify current user is admin or removing themselves
                ConversationMembership currentUserMembership = membershipRepository
                                .findByConversationIdAndUserId(conversationObjectId, currentUserObjectId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User is not a member of this conversation"));

                if (!currentUserId.equals(userIdToRemove) &&
                                currentUserMembership.getRole() != ConversationMembership.MemberRole.ADMIN) {
                        throw new IllegalArgumentException("Only admins can remove other members");
                }

                membershipRepository.deleteByConversationIdAndUserId(conversationObjectId, userToRemoveObjectId);
                log.info("Removed member: {} from conversation: {}", userIdToRemove, conversationId);
        }

        /**
         * Map conversation entity to response DTO
         */
        private ConversationResponse mapToResponse(Conversation conversation,
                        List<ConversationMembership> memberships, String currentUserId) {
                List<ConversationResponse.ConversationMemberResponse> memberResponses = memberships.stream()
                                .map(membership -> {
                                        // Fetch user details from UserRepository
                                        String userName = userRepository.findById(membership.getUserId())
                                                        .map(user -> user.getUserName())
                                                        .orElse("User " + membership.getUserId().toString());

                                        return ConversationResponse.ConversationMemberResponse.builder()
                                                        .userId(membership.getUserId().toString())
                                                        .userName(userName)
                                                        .role(membership.getRole())
                                                        .lastReadAt(membership.getLastReadAt())
                                                        .muted(membership.isMuted())
                                                        .joinedAt(membership.getJoinedAt())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                List<String> adminIdStrings = conversation.getAdminIds() != null ? conversation.getAdminIds().stream()
                                .map(ObjectId::toString)
                                .collect(Collectors.toList()) : List.of();

                return ConversationResponse.builder()
                                .id(conversation.getId().toString())
                                .type(conversation.getType())
                                .title(conversation.getTitle())
                                .members(memberResponses)
                                .ownerId(conversation.getOwnerId() != null ? conversation.getOwnerId().toString()
                                                : null)
                                .adminIds(adminIdStrings)
                                .createdAt(conversation.getCreatedAt())
                                .lastMessageAt(conversation.getLastMessageAt())
                                .isArchived(conversation.isArchived())
                                .unreadCount(calculateUnreadCount(conversation.getId(), currentUserId))
                                .build();
        }

        /**
         * Calculate unread message count for a user in a conversation
         */
        private int calculateUnreadCount(ObjectId conversationId, String userId) {
                try {
                        ObjectId userObjectId = new ObjectId(userId);

                        // Find the user's membership to get lastReadAt
                        ConversationMembership membership = membershipRepository
                                        .findByConversationIdAndUserId(conversationId, userObjectId)
                                        .orElse(null);

                        if (membership == null || membership.getLastReadAt() == null) {
                                // If no membership or never read, count all messages
                                return (int) messageRepository.count();
                        }

                        // Count messages created after user's last read time
                        return (int) messageRepository.countUnreadMessages(conversationId, membership.getLastReadAt());
                } catch (Exception e) {
                        log.warn("Error calculating unread count for conversation {} and user {}: {}",
                                        conversationId, userId, e.getMessage());
                        return 0;
                }
        }
}