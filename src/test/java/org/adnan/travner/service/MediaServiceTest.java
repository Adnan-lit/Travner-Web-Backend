package org.adnan.travner.service;

import org.adnan.travner.entry.MediaEntry;
import org.adnan.travner.entry.ProductEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.repository.MediaRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.ProductRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private ProductRepository productRepository;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private GridFsOperations gridFsOperations;
    @InjectMocks private MediaService mediaService;

    private UserEntry user;
    private ProductEntry product;
    private String productId;

    @BeforeEach
    void setUp() {
        user = UserEntry.builder().id(new ObjectId()).userName("alice").roles(List.of("USER")).build();
        productId = new ObjectId().toHexString();
        product = new ProductEntry();
        product.setId(new ObjectId(productId));
        product.setSellerId(user.getId().toHexString());
        product.setSellerUsername("alice");
        product.setImages(new ArrayList<>());

        // GridFsTemplate/GridFsOperations are @Autowired fields, not constructor args.
        org.springframework.test.util.ReflectionTestUtils.setField(mediaService, "gridFsTemplate", gridFsTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(mediaService, "gridFsOperations", gridFsOperations);
    }

    private static MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "photo.png", "image/png", out.toByteArray());
    }

    @Test
    void uploadProductMedia_appendsCanonicalUrlExactlyOnce() throws Exception {
        when(userRepository.findByuserName("alice")).thenReturn(user);
        when(productRepository.findById(new ObjectId(productId))).thenReturn(Optional.of(product));
        when(mediaRepository.save(any(MediaEntry.class))).thenAnswer(invocation -> {
            MediaEntry media = invocation.getArgument(0);
            if (media.getId() == null) {
                media.setId(new ObjectId());
            }
            return media;
        });
        when(productRepository.save(any(ProductEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(new ObjectId()).when(gridFsTemplate).store(
                any(InputStream.class), anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());

        var dto = mediaService.uploadMedia(pngFile(), "alice", "product", productId);

        assertEquals(1, product.getImages().size());
        assertEquals(dto.getDownloadUrl(), product.getImages().get(0));
        assertTrue(dto.getDownloadUrl().startsWith("/api/media/"));
        assertEquals("image/png", dto.getContentType());
    }

    @Test
    void uploadProductMedia_nonSellerIsRejectedBeforeStorage() throws Exception {
        product.setSellerId(new ObjectId().toHexString());
        product.setSellerUsername("someone-else");
        when(userRepository.findByuserName("alice")).thenReturn(user);
        when(productRepository.findById(new ObjectId(productId))).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> mediaService.uploadMedia(pngFile(), "alice", "product", productId));
        verifyNoInteractions(gridFsTemplate, mediaRepository);
    }

    @Test
    void deleteProductMedia_removesGridFsObjectEntryAndProductUrl() {
        ObjectId mediaId = new ObjectId();
        String mediaUrl = "/api/media/" + mediaId;
        MediaEntry media = MediaEntry.builder()
                .id(mediaId)
                .entityId(productId)
                .type("product")
                .filename("stored_photo.png")
                .gridFsId(new ObjectId().toHexString())
                .build();
        product.getImages().add(mediaUrl);
        when(mediaRepository.findByEntityIdAndType(productId, "product")).thenReturn(List.of(media));
        when(productRepository.findById(new ObjectId(productId))).thenReturn(Optional.of(product));

        mediaService.deleteProductMedia(productId, mediaUrl);

        verify(gridFsTemplate).delete(any(Query.class));
        verify(mediaRepository).delete(media);
        assertTrue(product.getImages().isEmpty());
    }

    @Test
    void deleteProductMedia_legacyFilenameUrlAlsoRemovesStoredCanonicalUrl() {
        ObjectId mediaId = new ObjectId();
        MediaEntry media = MediaEntry.builder()
                .id(mediaId)
                .entityId(productId)
                .type("product")
                .filename("stored_photo.png")
                .gridFsId(new ObjectId().toHexString())
                .build();
        product.getImages().add("/api/media/" + mediaId);
        when(mediaRepository.findByEntityIdAndType(productId, "product")).thenReturn(List.of(media));
        when(productRepository.findById(new ObjectId(productId))).thenReturn(Optional.of(product));

        mediaService.deleteProductMedia(productId, "/api/media/files/stored_photo.png");

        verify(mediaRepository).delete(media);
        assertTrue(product.getImages().isEmpty());
    }

    @Test
    void deleteProductMedia_rejectsUrlThatDoesNotBelongToThisProduct() {
        when(mediaRepository.findByEntityIdAndType(productId, "product")).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> mediaService.deleteProductMedia(productId, "/api/media/" + new ObjectId()));
        verify(gridFsTemplate, never()).delete(any(Query.class));
        verify(mediaRepository, never()).delete(any());
    }

    @Test
    void deleteMedia_keepsDatabaseEntryWhenGridFsDeletionFails() {
        MediaEntry media = MediaEntry.builder()
                .id(new ObjectId())
                .uploadedBy(user.getId().toHexString())
                .gridFsId(new ObjectId().toHexString())
                .build();
        when(userRepository.findByuserName("alice")).thenReturn(user);
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        doThrow(new RuntimeException("GridFS unavailable")).when(gridFsTemplate).delete(any(Query.class));

        assertThrows(RuntimeException.class, () -> mediaService.deleteMedia(media.getId().toHexString(), "alice"));
        verify(mediaRepository, never()).delete(any());
    }

    @Test
    void getMediaFile_setsNosniffAndSafeContentDisposition() throws Exception {
        ObjectId mediaId = new ObjectId();
        MediaEntry media = MediaEntry.builder()
                .id(mediaId)
                .filename("stored_photo.png")
                .originalFilename("evil\"\r\nname.png")
                .gridFsId(new ObjectId().toHexString())
                .contentType("image/png")
                .build();
        com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                mock(com.mongodb.client.gridfs.model.GridFSFile.class);
        GridFsResource resource = mock(GridFsResource.class);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFsFile);
        when(gridFsOperations.getResource(gridFsFile)).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ResponseEntity<org.springframework.core.io.InputStreamResource> response =
                mediaService.getMediaFile(mediaId.toHexString());

        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertFalse(disposition.contains("\r"));
        assertFalse(disposition.contains("\n"));
        assertFalse(disposition.contains("evil\""));
    }

    @Test
    void getMediaFileByName_setsNosniff() throws Exception {
        com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                mock(com.mongodb.client.gridfs.model.GridFSFile.class);
        GridFsResource resource = mock(GridFsResource.class);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFsFile);
        when(gridFsFile.getMetadata()).thenReturn(new Document("_contentType", "image/png"));
        when(gridFsOperations.getResource(gridFsFile)).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        ResponseEntity<org.springframework.core.io.InputStreamResource> response =
                mediaService.getMediaFileByName("stored_photo.png");

        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("inline; filename=\"stored_photo.png\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }
}
