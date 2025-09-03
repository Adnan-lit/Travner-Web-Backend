package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("debug")
@RequiredArgsConstructor
public class DebugController {

    private final PostRepository postRepository;

    @GetMapping("/post-count")
    public ResponseEntity<Map<String, Object>> getPostCount() {
        Map<String, Object> response = new HashMap<>();

        // Count all posts
        long totalCount = postRepository.count();
        response.put("totalPosts", totalCount);

        // Count published posts
        long publishedCount = postRepository.findByPublishedTrue(PageRequest.of(0, 1)).getTotalElements();
        response.put("publishedPosts", publishedCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-test-post")
    public ResponseEntity<Map<String, Object>> createTestPost() {
        Map<String, Object> response = new HashMap<>();

        // Create a test post with published=true
        PostEntry post = new PostEntry();
        post.setTitle("Test Post " + System.currentTimeMillis());
        post.setContent("This is a test post created for debugging");
        post.setLocation("Test Location");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setTags(new ArrayList<>());
        post.setMediaUrls(new ArrayList<>());
        post.setPublished(true); // Explicitly set to true

        PostEntry savedPost = postRepository.save(post);

        response.put("success", true);
        response.put("postId", savedPost.getId().toString());
        response.put("published", savedPost.isPublished());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/fix-posts")
    public ResponseEntity<Map<String, Object>> fixPosts() {
        Map<String, Object> response = new HashMap<>();
        List<PostEntry> allPosts = postRepository.findAll();

        int fixedCount = 0;
        for (PostEntry post : allPosts) {
            // Force all posts to be published for testing
            if (!post.isPublished()) {
                post.setPublished(true);
                postRepository.save(post);
                fixedCount++;
            }
        }

        response.put("totalPosts", allPosts.size());
        response.put("fixedPosts", fixedCount);

        return ResponseEntity.ok(response);
    }
}
