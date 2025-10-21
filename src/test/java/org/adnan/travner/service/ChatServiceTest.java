package org.adnan.travner.service;

import org.adnan.travner.dto.ChatConversationDTO;
import org.adnan.travner.dto.ChatMessageDTO;
import org.adnan.travner.dto.SendMessageDTO;
import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.entry.ChatMessage;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(testMessage);
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
        
        when(chatConversationRepository.findByParticipantIdsContainingAndIsActiveTrue("user1", any(Pageable.class)))
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
        
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc("conv1", any(Pageable.class)))
                .thenReturn(messagePage);

        // When
        Page<ChatMessageDTO> result = chatService.getConversationMessages("conv1", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("msg1", result.getContent().get(0).getId());
    }
}


