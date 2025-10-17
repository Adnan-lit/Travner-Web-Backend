package org.adnan.travner.service;

import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CommentRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.PostVoteRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.repository.MediaRepository;
import org.adnan.travner.entry.MediaEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostVoteRepository postVoteRepository;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private PostService postService;

    private UserEntry testUser;
    private PostEntry testPost;

    @BeforeEach
    void setUp() {
        testUser = new UserEntry();
        testUser.setId(new ObjectId());
        testUser.setUserName("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testPost = PostEntry.builder()
                .id(new ObjectId())
                .title("Test Post")
                .content("This is a test post content")
                .author(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(List.of("test", "post"))
                .upvotes(0)
                .downvotes(0)
                .comments(new ArrayList<>())
                .mediaUrls(new ArrayList<>())
                .published(true)
                .build();
    }

    @Test
    void testCreatePost_Success() {
        // Given
        PostRequest postRequest = PostRequest.builder()
                .title("Test Post")
                .content("This is a test post content")
                .location("Test Location")
                .tags(List.of("test", "post"))
                .published(true)
                .build();

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);

        // When
        var result = postService.createPost("testuser", postRequest);

        // Then
        assertNotNull(result);
        assertEquals("Test Post", result.getTitle());
        assertEquals("This is a test post content", result.getContent());
        verify(postRepository, times(1)).save(any(PostEntry.class));
    }

    @Test
    void testCreatePost_WithMediaIds() {
        // Given
        List<String> mediaIds = List.of(new ObjectId().toString(), new ObjectId().toString());
        PostRequest postRequest = PostRequest.builder()
                .title("Test Post")
                .content("This is a test post content")
                .location("Test Location")
                .tags(List.of("test", "post"))
                .published(true)
                .mediaIds(mediaIds)
                .build();

        MediaEntry media1 = MediaEntry.builder()
                .id(new ObjectId())
                .filename("test1.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .uploadedBy(testUser.getId().toString())
                .uploadedAt(LocalDateTime.now())
                .build();

        MediaEntry media2 = MediaEntry.builder()
                .id(new ObjectId())
                .filename("test2.jpg")
                .contentType("image/jpeg")
                .size(2048L)
                .uploadedBy(testUser.getId().toString())
                .uploadedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);
        when(mediaRepository.findById(any(ObjectId.class)))
                .thenReturn(Optional.of(media1))
                .thenReturn(Optional.of(media2));
        when(mediaRepository.save(any(MediaEntry.class))).thenReturn(media1).thenReturn(media2);

        // When
        var result = postService.createPost("testuser", postRequest);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getMediaUrls().size());
        verify(postRepository, times(2)).save(any(PostEntry.class)); // Initial save + update with media
        verify(mediaRepository, times(2)).save(any(MediaEntry.class));
    }

    @Test
    void testUpdatePost_Success() {
        // Given
        String postId = testPost.getId().toString();
        PostRequest postRequest = PostRequest.builder()
                .title("Updated Post")
                .content("This is updated content")
                .location("Updated Location")
                .tags(List.of("updated", "post"))
                .published(true)
                .build();

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.findById(any(ObjectId.class))).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);

        // When
        var result = postService.updatePost(postId, "testuser", postRequest);

        // Then
        assertNotNull(result);
        assertEquals("Updated Post", result.getTitle());
        verify(postRepository, times(1)).save(any(PostEntry.class));
    }

    @Test
    void testUpdatePost_WithMediaIds() {
        // Given
        String postId = testPost.getId().toString();
        List<String> mediaIds = List.of(new ObjectId().toString());
        PostRequest postRequest = PostRequest.builder()
                .title("Updated Post")
                .content("This is updated content")
                .location("Updated Location")
                .tags(List.of("updated", "post"))
                .published(true)
                .mediaIds(mediaIds)
                .build();

        MediaEntry media = MediaEntry.builder()
                .id(new ObjectId())
                .filename("test.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .uploadedBy(testUser.getId().toString())
                .uploadedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.findById(any(ObjectId.class))).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);
        when(mediaRepository.findByPostId(any(ObjectId.class))).thenReturn(new ArrayList<>());
        when(mediaRepository.findById(any(ObjectId.class))).thenReturn(Optional.of(media));
        when(mediaRepository.save(any(MediaEntry.class))).thenReturn(media);

        // When
        var result = postService.updatePost(postId, "testuser", postRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMediaUrls().size());
        verify(postRepository, times(1)).save(any(PostEntry.class));
        verify(mediaRepository, times(1)).save(any(MediaEntry.class));
    }

    @Test
    void testDeletePost_Success() {
        // Given
        String postId = testPost.getId().toString();
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.findById(any(ObjectId.class))).thenReturn(Optional.of(testPost));
        when(mediaRepository.findByPostId(any(ObjectId.class))).thenReturn(new ArrayList<>());

        // When
        assertDoesNotThrow(() -> postService.deletePost(postId, "testuser"));

        // Then
        verify(postRepository, times(1)).delete(any(PostEntry.class));
    }

    @Test
    void testGetPostById_Success() {
        // Given
        String postId = testPost.getId().toString();
        when(postRepository.findById(any(ObjectId.class))).thenReturn(Optional.of(testPost));
        when(commentRepository.countByPostId(any(ObjectId.class))).thenReturn(5L);

        // When
        var result = postService.getPostById(postId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getCommentCount());
        verify(postRepository, times(1)).findById(any(ObjectId.class));
    }

    @Test
    void testGetPostById_NotFound() {
        // Given
        String postId = new ObjectId().toString();
        when(postRepository.findById(any(ObjectId.class))).thenReturn(Optional.empty());

        // When
        var result = postService.getPostById(postId);

        // Then
        assertFalse(result.isPresent());
        verify(postRepository, times(1)).findById(any(ObjectId.class));
    }
}