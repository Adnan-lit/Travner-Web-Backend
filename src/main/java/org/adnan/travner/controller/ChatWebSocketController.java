package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.chat.ChatEventDTO;
import org.adnan.travner.dto.chat.SendMessageRequest;
import org.adnan.travner.dto.chat.TypingIndicatorRequest;
import org.adnan.travner.service.MessageService;
import org.adnan.travner.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

/**
 * WebSocket controller for real-time chat functionality
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

        private final MessageService messageService;
        private final SimpMessagingTemplate messagingTemplate;
        private final UserService userService;

        /**
         * Get display name for user (username or fallback to ID)
         */
        private String getUserDisplayName(String userId) {
                try {
                        return userService.getByUsername(userId).getUserName();
                } catch (Exception e) {
                        log.warn("Could not fetch username for user: {}, using ID as fallback", userId);
                        return userId;
                }
        }

        /**
         * Handle real-time message sending
         */
        @MessageMapping("/chat.sendMessage")
        public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
                log.debug("WebSocket message received from user: {} for conversation: {}",
                                principal.getName(), request.getConversationId());

                try {
                        // Send message through service
                        var messageResponse = messageService.sendMessage(request, principal.getName());

                        // Create real-time event
                        var event = ChatEventDTO.builder()
                                        .type(ChatEventDTO.EventType.MESSAGE_SENT)
                                        .conversationId(request.getConversationId())
                                        .userId(principal.getName())
                                        .userName(getUserDisplayName(principal.getName()))
                                        .data(messageResponse)
                                        .timestamp(Instant.now())
                                        .build();

                        // Broadcast to the conversation topic
                        messagingTemplate.convertAndSend(
                                        "/topic/conversation/" + request.getConversationId(),
                                        event);

                } catch (Exception e) {
                        log.error("Error sending message via WebSocket: {}", e.getMessage());
                        throw new RuntimeException("Failed to send message");
                }
        }

        /**
         * Handle typing indicator
         */
        @MessageMapping("/chat.typing")
        public void handleTypingIndicator(@Payload TypingIndicatorRequest request, Principal principal) {
                log.debug("Typing indicator from user: {} in conversation: {} - typing: {}",
                                principal.getName(), request.getConversationId(), request.isTyping());

                ChatEventDTO.EventType eventType = request.isTyping() ? ChatEventDTO.EventType.USER_TYPING
                                : ChatEventDTO.EventType.USER_STOPPED_TYPING;

                ChatEventDTO event = ChatEventDTO.builder()
                                .type(eventType)
                                .conversationId(request.getConversationId())
                                .userId(principal.getName())
                                .userName(getUserDisplayName(principal.getName()))
                                .timestamp(Instant.now())
                                .build();

                // Send to all users in the conversation except the sender
                messagingTemplate.convertAndSend(
                                "/topic/conversation/" + request.getConversationId(),
                                event);
        }

        /**
         * Handle user joining a conversation (subscribing to updates)
         */
        @SubscribeMapping("/topic/conversation/{conversationId}")
        public ChatEventDTO handleUserJoined(@DestinationVariable String conversationId, Principal principal) {
                log.debug("User: {} joined conversation: {}", principal.getName(), conversationId);

                ChatEventDTO event = ChatEventDTO.builder()
                                .type(ChatEventDTO.EventType.USER_JOINED_CONVERSATION)
                                .conversationId(conversationId)
                                .userId(principal.getName())
                                .userName(getUserDisplayName(principal.getName()))
                                .timestamp(Instant.now())
                                .build();

                // Notify other users about the join
                messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, event);

                return event;
        }

        /**
         * Handle user presence updates
         */
        @MessageMapping("/chat.presence")
        public void handlePresenceUpdate(@Payload ChatEventDTO presenceEvent, Principal principal) {
                log.debug("Presence update from user: {} - status: {}", principal.getName(), presenceEvent.getType());

                presenceEvent.setUserId(principal.getName());
                presenceEvent.setUserName(getUserDisplayName(principal.getName()));
                presenceEvent.setTimestamp(Instant.now());

                // Broadcast presence to all user's conversations
                // This could be optimized to only send to relevant conversations
                messagingTemplate.convertAndSendToUser(
                                principal.getName(),
                                "/queue/presence",
                                presenceEvent);
        }

        /**
         * Handle message read events
         */
        @MessageMapping("/chat.messageRead")
        public void handleMessageRead(@Payload ChatEventDTO readEvent, Principal principal) {
                log.debug("Message read event from user: {} in conversation: {}",
                                principal.getName(), readEvent.getConversationId());

                readEvent.setUserId(principal.getName());
                readEvent.setUserName(getUserDisplayName(principal.getName()));
                readEvent.setTimestamp(Instant.now());
                readEvent.setType(ChatEventDTO.EventType.MESSAGE_READ);

                // Send read receipt to conversation
                messagingTemplate.convertAndSend(
                                "/topic/conversation/" + readEvent.getConversationId(),
                                readEvent);
        }

        /**
         * Send notification to specific user
         */
        public void sendUserNotification(String userId, ChatEventDTO event) {
                log.debug("Sending notification to user: {} - type: {}", userId, event.getType());

                messagingTemplate.convertAndSendToUser(
                                userId,
                                "/queue/notifications",
                                event);
        }

        /**
         * Send event to conversation participants
         */
        public void sendConversationEvent(String conversationId, ChatEventDTO event) {
                log.debug("Sending event to conversation: {} - type: {}", conversationId, event.getType());

                messagingTemplate.convertAndSend(
                                "/topic/conversation/" + conversationId,
                                event);
        }
}