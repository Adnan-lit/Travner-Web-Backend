package org.adnan.travner.service;

import org.adnan.travner.dto.chat.ChatAttachmentDTO;
import org.adnan.travner.entry.ChatAttachment;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.repository.ChatAttachmentRepository;
import org.adnan.travner.repository.ChatConversationRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAttachmentServiceTest {

    @Mock
    private GridFsTemplate gridFsTemplate;
    @Mock
    private ChatAttachmentRepository attachmentRepository;
    @Mock
    private ChatConversationRepository conversationRepository;
    @Mock
    private ChatService chatService;
    @InjectMocks
    private ChatAttachmentService service;

    private ChatAttachment attachment;

    @BeforeEach
    void setUp() {
        attachment = ChatAttachment.builder()
                .id("att1")
                .conversationId("conv1")
                .uploaderId("user1")
                .gridFsId(new ObjectId().toString())
                .storedFilename("uuid_notes.txt")
                .originalFilename("notes.txt")
                .contentType("text/plain")
                .size(10)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void upload_storesValidatedFileAndReturnsRealDownloadUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello\n".getBytes(StandardCharsets.UTF_8));
        doReturn(new ObjectId()).when(gridFsTemplate).store(
                any(InputStream.class), anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
        when(attachmentRepository.save(any(ChatAttachment.class))).thenAnswer(invocation -> {
            ChatAttachment saved = invocation.getArgument(0);
            saved.setId("att1");
            return saved;
        });

        ChatAttachmentDTO dto = service.upload(file, "conv1", "user1");

        verify(chatService).assertParticipant("conv1", "user1");
        ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);
        verify(gridFsTemplate).store(any(InputStream.class), filename.capture(), eq("text/plain"),
                org.mockito.ArgumentMatchers.<Object>any());
        assertTrue(filename.getValue().endsWith("_notes.txt"));
        assertEquals("/api/chat/attachments/att1/download", dto.getDownloadUrl());
        assertEquals("notes.txt", dto.getFileName());
        assertEquals(6L, dto.getSize());
    }

    @Test
    void upload_nonParticipantIsRejectedBeforeAnyStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        doThrow(new ForbiddenException("not a participant"))
                .when(chatService).assertParticipant("conv1", "intruder");

        assertThrows(ForbiddenException.class, () -> service.upload(file, "conv1", "intruder"));
        verifyNoInteractions(gridFsTemplate, attachmentRepository);
    }

    @Test
    void upload_dotlessFilenameIsHandled() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "README", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        doReturn(new ObjectId()).when(gridFsTemplate).store(
                any(InputStream.class), anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
        when(attachmentRepository.save(any(ChatAttachment.class))).thenReturn(attachment);

        assertDoesNotThrow(() -> service.upload(file, "conv1", "user1"));
        ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);
        verify(gridFsTemplate).store(any(InputStream.class), filename.capture(), anyString(),
                org.mockito.ArgumentMatchers.<Object>any());
        assertTrue(filename.getValue().endsWith("_README"));
    }

    @Test
    void download_nonParticipantCannotReadAttachment() {
        when(attachmentRepository.findById("att1")).thenReturn(Optional.of(attachment));
        doThrow(new ForbiddenException("not a participant"))
                .when(chatService).assertParticipant("conv1", "intruder");

        assertThrows(ForbiddenException.class, () -> service.download("att1", "intruder"));
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void delete_nonUploaderIsRejectedAndNothingIsRemoved() {
        when(attachmentRepository.findById("att1")).thenReturn(Optional.of(attachment));

        assertThrows(ForbiddenException.class, () -> service.delete("att1", "user2"));
        verifyNoInteractions(gridFsTemplate);
        verify(attachmentRepository, never()).delete(any());
    }

    @Test
    void delete_uploaderRemovesGridFsObjectAndRecord() {
        when(attachmentRepository.findById("att1")).thenReturn(Optional.of(attachment));

        service.delete("att1", "user1");

        verify(gridFsTemplate).delete(any(Query.class));
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void deleteOrphans_removesAttachmentsForMissingConversations() {
        when(attachmentRepository.findAll()).thenReturn(List.of(attachment));
        when(conversationRepository.existsById("conv1")).thenReturn(false);

        assertEquals(1, service.deleteOrphans());
        verify(gridFsTemplate).delete(any(Query.class));
        verify(attachmentRepository).delete(attachment);
    }
}
