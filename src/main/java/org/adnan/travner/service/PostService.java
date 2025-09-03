package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CommentRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public PostDTO createPost(String username, PostRequest postRequest) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        PostEntry post = PostEntry.builder()
                .title(postRequest.getTitle())
                .content(postRequest.getContent())
                .location(postRequest.getLocation())
                .author(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(postRequest.getTags())
                .upvotes(0)
                .downvotes(0)
                .comments(new ArrayList<>())
                .mediaUrls(new ArrayList<>())
                .published(postRequest.isPublished())
                .build();

        PostEntry savedPost = postRepository.save(post);
        return convertToDTO(savedPost, 0);
    }

    public Page<PostDTO> getAllPublishedPosts(Pageable pageable) {
        // Get published posts using the derived query method
        Page<PostEntry> posts = postRepository.findByPublishedTrue(pageable);

        // Convert posts to DTOs with comment counts
        return posts.map(post -> {
            long commentCount = commentRepository.countByPostId(post.getId());
            return convertToDTO(post, commentCount);
        });
    }

    public Optional<PostDTO> getPostById(String id) {
        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(id));
        return postOptional.map(post -> {
            long commentCount = commentRepository.countByPostId(post.getId());
            return convertToDTO(post, commentCount);
        });
    }

    public Page<PostDTO> getPostsByUser(String username, Pageable pageable) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return postRepository.findByAuthor_Id(user.getId(), pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return convertToDTO(post, commentCount);
                });
    }

    public Page<PostDTO> searchPosts(String query, Pageable pageable) {
        return postRepository.searchPosts(query, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return convertToDTO(post, commentCount);
                });
    }

    public Page<PostDTO> getPostsByLocation(String location, Pageable pageable) {
        return postRepository.findByLocationContainingIgnoreCase(location, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return convertToDTO(post, commentCount);
                });
    }

    public Page<PostDTO> getPostsByTags(List<String> tags, Pageable pageable) {
        return postRepository.findByTagsIn(tags, pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return convertToDTO(post, commentCount);
                });
    }

    public PostDTO updatePost(String id, String username, PostRequest postRequest) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(id));
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        PostEntry post = postOptional.get();

        // Check if the user is the author of the post
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to update this post");
        }

        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setLocation(postRequest.getLocation());
        post.setTags(postRequest.getTags());
        post.setUpdatedAt(LocalDateTime.now());
        post.setPublished(postRequest.isPublished());

        PostEntry updatedPost = postRepository.save(post);
        long commentCount = commentRepository.countByPostId(updatedPost.getId());
        return convertToDTO(updatedPost, commentCount);
    }

    public void deletePost(String id, String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(id));
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        PostEntry post = postOptional.get();

        // Check if the user is the author or an admin
        if (!post.getAuthor().getId().equals(user.getId()) &&
                (user.getRoles() == null || !user.getRoles().contains("ROLE_ADMIN"))) {
            throw new RuntimeException("You are not authorized to delete this post");
        }

        postRepository.delete(post);
    }

    public PostDTO updateVote(String id, String username, boolean isUpvote) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(id));
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        PostEntry post = postOptional.get();

        if (isUpvote) {
            post.setUpvotes(post.getUpvotes() + 1);
        } else {
            post.setDownvotes(post.getDownvotes() + 1);
        }

        PostEntry updatedPost = postRepository.save(post);
        long commentCount = commentRepository.countByPostId(updatedPost.getId());
        return convertToDTO(updatedPost, commentCount);
    }

    private PostDTO convertToDTO(PostEntry post, long commentCount) {
        UserEntry author = post.getAuthor();
        UserSummaryDTO authorDTO = UserSummaryDTO.builder()
                .id(author.getId().toString())
                .userName(author.getUserName())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .build();

        return PostDTO.builder()
                .id(post.getId().toString())
                .title(post.getTitle())
                .content(post.getContent())
                .location(post.getLocation())
                .mediaUrls(post.getMediaUrls())
                .author(authorDTO)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .tags(post.getTags())
                .upvotes(post.getUpvotes())
                .downvotes(post.getDownvotes())
                .commentCount((int) commentCount)
                .published(post.isPublished())
                .build();
    }
}
