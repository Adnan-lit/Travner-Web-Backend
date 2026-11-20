package org.adnan.travner.controller;

import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.dto.MediaDTO;
import org.adnan.travner.exception.ExceptionResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class PostControllerTest {

    private static final String POST_ID = "507f191e810c19729de860ea";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void allowRequests() {
        when(rateLimiter.isApiAllowed(anyString())).thenReturn(true);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreatePost() throws Exception {
        // Given
        PostDTO postDTO = PostDTO.builder()
                .id("1")
                .title("Test Post")
                .content("Test content")
                .author(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(List.of("test"))
                .upvotes(0)
                .downvotes(0)
                .commentCount(0)
                .mediaUrls(List.of())
                .published(true)
                .build();

        when(postService.createPost(eq("testuser"), any(PostRequest.class))).thenReturn(postDTO);

        // When & Then
        mockMvc.perform(post("/api/posts")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Test Post",
                            "content": "Test content",
                            "tags": ["test"],
                            "published": true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Post"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetPostById() throws Exception {
        // Given
        PostDTO postDTO = PostDTO.builder()
                .id("1")
                .title("Test Post")
                .content("Test content")
                .author(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(List.of("test"))
                .upvotes(0)
                .downvotes(0)
                .commentCount(5)
                .mediaUrls(List.of())
                .published(true)
                .build();

        when(postService.getPostById(POST_ID)).thenReturn(java.util.Optional.of(postDTO));

        // When & Then
        mockMvc.perform(get("/api/posts/" + POST_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.commentCount").value(5));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllPostsRejectsInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/posts").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllPostsRejectsInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/posts").param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllPostsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/posts").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllPostsRejectsZeroSize() throws Exception {
        mockMvc.perform(get("/api/posts").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllPostsCapsOversizedPage() throws Exception {
        when(postService.getAllPublishedPosts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/posts").param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).getAllPublishedPosts(captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getPostByIdRejectsMalformedIdWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts/not-an-object-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void errorResponsesCarryStableCodeAndCorrelationId() throws Exception {
        mockMvc.perform(get("/api/posts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void unexpectedServiceFailureDoesNotLeakInternalMessage() throws Exception {
        when(postService.getAllPublishedPosts(any(Pageable.class)))
                .thenThrow(new IllegalStateException("mongodb://user:secret@internal-host"));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(ExceptionResponses.INTERNAL_MESSAGE))
                .andExpect(content().string(not(containsString("internal-host"))))
                .andExpect(content().string(not(containsString("secret"))));
    }
}