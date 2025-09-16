package org.adnan.travner.service;

import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CommentRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostService
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    private UserEntry testUser;
    private PostEntry testPost;
    private PostRequest testPostRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserEntry();
        testUser.setId(new ObjectId());
        testUser.setUserName("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setRoles(List.of("USER"));

        // Create test post
        testPost = PostEntry.builder()
                .id(new ObjectId())
                .title("Test Post")
                .content("This is a test post content")
                .location("Test Location")
                .author(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(Arrays.asList("test", "example"))
                .upvotes(0)
                .downvotes(0)
                .comments(new ArrayList<>())
                .mediaUrls(new ArrayList<>())
                .published(true)
                .build();

        // Create test post request
        testPostRequest = PostRequest.builder()
                .title("Test Post")
                .content("This is a test post content")
                .location("Test Location")
                .tags(Arrays.asList("test", "example"))
                .published(true)
                .build();
    }

    @Test
    void createPost_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);

        // When
        PostDTO result = postService.createPost("testuser", testPostRequest);

        // Then
        assertNotNull(result);
        assertEquals("Test Post", result.getTitle());
        assertEquals("This is a test post content", result.getContent());
        assertEquals("Test Location", result.getLocation());
        assertTrue(result.isPublished());
        
        verify(userRepository).findByuserName("testuser");
        verify(postRepository).save(any(PostEntry.class));
    }

    @Test
    void createPost_UserNotFound() {
        // Given
        when(userRepository.findByuserName("nonexistent")).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            postService.createPost("nonexistent", testPostRequest);
        });
        
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByuserName("nonexistent");
        verify(postRepository, never()).save(any());
    }

    @Test
    void getAllPublishedPosts_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<PostEntry> posts = List.of(testPost);
        Page<PostEntry> postPage = new PageImpl<>(posts, pageable, 1);
        
        when(postRepository.findByPublishedTrue(pageable)).thenReturn(postPage);
        when(commentRepository.countByPostId(any(ObjectId.class))).thenReturn(0L);

        // When
        Page<PostDTO> result = postService.getAllPublishedPosts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Post", result.getContent().get(0).getTitle());
        
        verify(postRepository).findByPublishedTrue(pageable);
        verify(commentRepository).countByPostId(any(ObjectId.class));
    }

    @Test
    void getPostById_Success() {
        // Given
        ObjectId postId = testPost.getId();
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(commentRepository.countByPostId(postId)).thenReturn(3L);

        // When
        Optional<PostDTO> result = postService.getPostById(postId.toString());

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Post", result.get().getTitle());
        assertEquals(3L, result.get().getCommentCount());
        
        verify(postRepository).findById(postId);
        verify(commentRepository).countByPostId(postId);
    }

    @Test
    void getPostById_NotFound() {
        // Given
        ObjectId postId = new ObjectId();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When
        Optional<PostDTO> result = postService.getPostById(postId.toString());

        // Then
        assertFalse(result.isPresent());
        verify(postRepository).findById(postId);
        verify(commentRepository, never()).countByPostId(any());
    }

    @Test
    void updatePost_Success() {
        // Given
        String postId = testPost.getId().toString();
        PostRequest updateRequest = PostRequest.builder()
                .title("Updated Title")
                .content("Updated content")
                .location("Updated Location")
                .tags(Arrays.asList("updated", "test"))
                .published(true)
                .build();

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(PostEntry.class))).thenReturn(testPost);
        when(commentRepository.countByPostId(any(ObjectId.class))).thenReturn(2L);

        // When
        PostDTO result = postService.updatePost(postId, "testuser", updateRequest);

        // Then
        assertNotNull(result);
        verify(userRepository).findByuserName("testuser");
        verify(postRepository).findById(testPost.getId());
        verify(postRepository).save(any(PostEntry.class));
    }

    @Test
    void updatePost_UserNotAuthorized() {
        // Given
        UserEntry otherUser = new UserEntry();
        otherUser.setId(new ObjectId());
        otherUser.setUserName("otheruser");

        when(userRepository.findByuserName("otheruser")).thenReturn(otherUser);
        when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            postService.updatePost(testPost.getId().toString(), "otheruser", testPostRequest);
        });

        assertEquals("You are not authorized to update this post", exception.getMessage());
        verify(postRepository, never()).save(any());
    }
}