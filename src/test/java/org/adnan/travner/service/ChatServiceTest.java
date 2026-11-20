package org.adnan.travner.service;

import org.adnan.travner.dto.ChatConversationDTO;
import org.adnan.travner.dto.ChatMessageDTO;
import org.adnan.travner.dto.SendMessageDTO;
import org.adnan.travner.dto.chat.ChatParticipantDTO;
import org.adnan.travner.entry.ChatAttachment;
import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.entry.ChatMessage;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.repository.ChatAttachmentRepository;
import org.adnan.travner.repository.ChatConversationRepository;
import org.adnan.travner.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for ChatService
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private ChatAttachmentRepository chatAttachmentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatService chatService;

    private ChatConversation testConversation;
    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        testConversation = ChatConversation.builder()
                .id("conv1")
                .participantIds(Arrays.asList("user1", "user2"))
                .type("DIRECT")
                .title("Test Conversation")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .unreadCount(0)
                .build();

        testMessage = ChatMessage.builder()
                .id("msg1")
                .conversationId("conv1")
                .senderId("user1")
                .senderUsername("testuser")
                .content("Hello, world!")
                .messageType("TEXT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isEdited(false)
                .readBy(Arrays.asList("user1"))
                .status("DELIVERED")
                .build();
    }

    @Test
    void testGetOrCreateDirectConversation_ExistingConversation() {
        // Given
        when(chatConversationRepository.findDirectConversationBetweenUsers("user1", "user2"))
                .thenReturn(Optional.of(testConversation));

        // When
        ChatConversationDTO result = chatService.getOrCreateDirectConversation("user1", "user2");

        // Then
        assertNotNull(result);
        assertEquals("conv1", result.getId());
        assertEquals("DIRECT", result.getType());
        verify(chatConversationRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateDirectConversation_NewConversation() {
        // Given
        when(chatConversationRepository.findDirectConversationBetweenUsers("user1", "user2"))
                .thenReturn(Optional.empty());
        when(chatConversationRepository.save(any(ChatConversation.class)))
                .thenReturn(testConversation);

        // When
        ChatConversationDTO result = chatService.getOrCreateDirectConversation("user1", "user2");

        // Then
        assertNotNull(result);
        assertEquals("conv1", result.getId());
        verify(chatConversationRepository).save(any(ChatConversation.class));
    }

    @Test
    void testSendMessage() {
        // Given
        SendMessageDTO request = SendMessageDTO.builder()
                .conversationId("conv1")
                .content("Test message")
                .messageType("TEXT")
                .build();

        when(chatConversationRepository.findById("conv1"))
                .thenReturn(Optional.of(testConversation));
        // Return the message built by the service (with an assigned id) so the
        // assertion checks the request content rather than the fixture's content.
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage saved = invocation.getArgument(0);
                    saved.setId("msg1");
                    return saved;
                });
        when(chatConversationRepository.save(any(ChatConversation.class)))
                .thenReturn(testConversation);

        // When
        ChatMessageDTO result = chatService.sendMessage(request, "user1");

        // Then
        assertNotNull(result);
        assertEquals("msg1", result.getId());
        assertEquals("Test message", result.getContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(chatConversationRepository).save(any(ChatConversation.class));
    }

    @Test
    void testGetUserConversations() {
        // Given
        List<ChatConversation> conversations = Arrays.asList(testConversation);
        Page<ChatConversation> conversationPage = new PageImpl<>(conversations);
        
        when(chatConversationRepository.findByParticipantIdsContainingAndIsActiveTrue(eq("user1"), any(Pageable.class)))
                .thenReturn(conversationPage);

        // When
        Page<ChatConversationDTO> result = chatService.getUserConversations("user1", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("conv1", result.getContent().get(0).getId());
    }

    @Test
    void testGetConversationMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(testMessage);
        Page<ChatMessage> messagePage = new PageImpl<>(messages);
        
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv1"), any(Pageable.class)))
                .thenReturn(messagePage);

        // When
        Page<ChatMessageDTO> result = chatService.getConversationMessages("conv1", "user1", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("msg1", result.getContent().get(0).getId());
    }

    @Test
    void nonParticipantCannotReadMessages() {
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));

        assertThrows(ForbiddenException.class,
                () -> chatService.getConversationMessages("conv1", "intruder", Pageable.unpaged()));
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void nonParticipantCannotReadConversationOrMarkRead() {
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));

        assertThrows(ForbiddenException.class,
                () -> chatService.getConversationById("conv1", "intruder"));
        assertThrows(ForbiddenException.class,
                () -> chatService.markMessagesAsRead("conv1", "intruder"));
        verifyNoInteractions(chatMessageRepository);
    }

    // P2-02: resolved participant summaries, per-user unread counts and ID identity.

    @Test
    void conversationDto_exposesResolvedParticipantsAndPerUserUnreadCounts() {
        when(chatConversationRepository.findByParticipantIdsContainingAndIsActiveTrue(
                eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testConversation)));
        when(userService.getById("user1")).thenReturn(user("alice", "Alice", "A"));
        when(userService.getById("user2")).thenReturn(user("bob", "Bob", "B"));
        when(chatMessageRepository.countByConversationIdAndReadByNotContaining("conv1", "user1")).thenReturn(0L);
        when(chatMessageRepository.countByConversationIdAndReadByNotContaining("conv1", "user2")).thenReturn(3L);

        ChatConversationDTO dto = chatService
                .getUserConversations("user1", Pageable.unpaged()).getContent().get(0);

        assertEquals(2, dto.getParticipants().size());
        ChatParticipantDTO alice = dto.getParticipants().get(0);
        assertEquals("user1", alice.getId());
        assertEquals("alice", alice.getUsername());
        assertEquals("Alice A", alice.getDisplayName());
        assertEquals(Map.of("user1", 0, "user2", 3), dto.getUnreadCounts());
        assertEquals(0, dto.getUnreadCount(), "legacy count must reflect the requesting user");
    }

    @Test
    void messageDto_resolvesSenderWithoutUnknownUserFallback() {
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testMessage)));
        when(userService.getById("user1")).thenReturn(user("alice", "Alice", "A"));

        ChatMessageDTO dto = chatService
                .getConversationMessages("conv1", "user1", Pageable.unpaged()).getContent().get(0);

        assertEquals("Alice A", dto.getSenderUsername());
        assertNotNull(dto.getSender());
        assertEquals("alice", dto.getSender().getUsername());
        assertEquals("Alice A", dto.getSender().getDisplayName());
        assertEquals("user1", dto.getSender().getId());
        assertNotEquals("Unknown User", dto.getSenderUsername());
    }

    @Test
    void messageDto_fallsBackToStoredNameForDeletedAccount() {
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testMessage)));
        when(userService.getById("user1")).thenReturn(null);

        ChatMessageDTO dto = chatService
                .getConversationMessages("conv1", "user1", Pageable.unpaged()).getContent().get(0);

        assertEquals("testuser", dto.getSenderUsername());
        assertNotEquals("Unknown User", dto.getSenderUsername());
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        SendMessageDTO request = SendMessageDTO.builder()
                .conversationId("conv1").content("hi").messageType("TEXT").build();
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));

        assertThrows(ForbiddenException.class, () -> chatService.sendMessage(request, "intruder"));
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void markMessagesAsRead_setsReadWhenEveryParticipantHasRead() {
        testConversation.setParticipantIds(List.of("user1", "user2"));
        testMessage.setReadBy(new java.util.ArrayList<>(List.of("user2")));
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatMessageRepository.findByConversationIdAndReadByNotContaining("conv1", "user1"))
                .thenReturn(List.of(testMessage));

        chatService.markMessagesAsRead("conv1", "user1");

        assertEquals("READ", testMessage.getStatus());
        assertTrue(testMessage.getReadBy().containsAll(List.of("user1", "user2")));
    }

    @Test
    void sendMessage_rejectsAttachmentFromAnotherConversation() {
        ChatAttachment foreign = ChatAttachment.builder()
                .id("att1").conversationId("other").uploaderId("user1").build();
        SendMessageDTO request = SendMessageDTO.builder()
                .conversationId("conv1").content("hi").messageType("FILE").attachmentId("att1").build();
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatAttachmentRepository.findById("att1")).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class, () -> chatService.sendMessage(request, "user1"));
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void sendMessage_rejectsAttachmentUploadedByAnotherUser() {
        ChatAttachment foreign = ChatAttachment.builder()
                .id("att1").conversationId("conv1").uploaderId("user2").build();
        SendMessageDTO request = SendMessageDTO.builder()
                .conversationId("conv1").content("hi").messageType("FILE").attachmentId("att1").build();
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatAttachmentRepository.findById("att1")).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class, () -> chatService.sendMessage(request, "user1"));
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void sendMessage_storesAttachmentAndResolvesMetadataInDto() {
        ChatAttachment owned = ChatAttachment.builder()
                .id("att1").conversationId("conv1").uploaderId("user1")
                .originalFilename("notes.txt").contentType("text/plain").size(5).build();
        SendMessageDTO request = SendMessageDTO.builder()
                .conversationId("conv1").content("see file").messageType("FILE").attachmentId("att1").build();
        when(chatConversationRepository.findById("conv1")).thenReturn(Optional.of(testConversation));
        when(chatAttachmentRepository.findById("att1")).thenReturn(Optional.of(owned));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageDTO result = chatService.sendMessage(request, "user1");

        assertEquals("att1", result.getAttachmentId());
        assertNotNull(result.getAttachment());
        assertEquals("notes.txt", result.getAttachment().getFileName());
        assertEquals("/api/chat/attachments/att1/download", result.getAttachment().getDownloadUrl());
    }

    private UserEntry user(String username, String first, String last) {
        UserEntry entry = new UserEntry();
        entry.setUserName(username);
        entry.setFirstName(first);
        entry.setLastName(last);
        return entry;
    }
}


