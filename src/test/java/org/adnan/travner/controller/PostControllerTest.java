package org.adnan.travner.controller;

import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.dto.MediaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @MockBean
    private MediaService mediaService;

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

        when(postService.getPostById("1")).thenReturn(java.util.Optional.of(postDTO));

        // When & Then
        mockMvc.perform(get("/api/posts/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.commentCount").value(5));
    }
}