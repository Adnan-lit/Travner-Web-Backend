package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.PostRequest;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.PostEntry;
import org.adnan.travner.entry.PostVoteEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CommentRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.PostVoteRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PostVoteRepository postVoteRepository;

    @Transactional
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

    @Transactional
    public PostDTO updatePost(String postId, String username, PostRequest postRequest) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(postId));
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

    @Transactional
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
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        boolean isAuthor = post.getAuthor().getId().equals(user.getId());

        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("You are not authorized to delete this post");
        }

        // Delete associated votes when deleting post
        postVoteRepository.findByPostId(new ObjectId(id)).forEach(postVoteRepository::delete);

        postRepository.delete(post);
    }

    /**
     * Admin method to get all posts (including unpublished)
     */
    public Page<PostDTO> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return convertToDTO(post, commentCount);
                });
    }

    /**
     * Get total post count for statistics
     */
    public long getPostCount() {
        return postRepository.count();
    }

    @Transactional
    public PostDTO updateVote(String id, String username, boolean isUpvote) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        ObjectId postId = new ObjectId(id);
        Optional<PostEntry> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        PostEntry post = postOptional.get();
        PostVoteEntry.VoteType newVoteType = isUpvote ? PostVoteEntry.VoteType.UPVOTE : PostVoteEntry.VoteType.DOWNVOTE;

        // Check if user has already voted
        Optional<PostVoteEntry> existingVote = postVoteRepository.findByPostIdAndUserId(postId, user.getId());

        if (existingVote.isPresent()) {
            PostVoteEntry vote = existingVote.get();
            if (vote.getVoteType() == newVoteType) {
                // User is trying to vote the same way again - remove the vote (toggle off)
                postVoteRepository.delete(vote);
            } else {
                // User is changing their vote
                vote.setVoteType(newVoteType);
                vote.setCreatedAt(LocalDateTime.now());
                postVoteRepository.save(vote);
            }
        } else {
            // New vote
            PostVoteEntry newVote = PostVoteEntry.builder()
                    .postId(postId)
                    .userId(user.getId())
                    .voteType(newVoteType)
                    .createdAt(LocalDateTime.now())
                    .build();
            postVoteRepository.save(newVote);
        }

        // Update post vote counts
        long upvotes = postVoteRepository.countByPostIdAndVoteType(postId, PostVoteEntry.VoteType.UPVOTE);
        long downvotes = postVoteRepository.countByPostIdAndVoteType(postId, PostVoteEntry.VoteType.DOWNVOTE);

        post.setUpvotes((int) upvotes);
        post.setDownvotes((int) downvotes);

        PostEntry updatedPost = postRepository.save(post);
        long commentCount = commentRepository.countByPostId(updatedPost.getId());
        return convertToDTO(updatedPost, commentCount);
    }

    private PostDTO convertToDTO(PostEntry post, long commentCount) {
        UserEntry author = post.getAuthor();
        UserSummaryDTO authorDTO = null;
        if (author != null) {
            authorDTO = UserSummaryDTO.builder()
                    .id(author.getId() != null ? author.getId().toString() : null)
                    .userName(author.getUserName())
                    .firstName(author.getFirstName())
                    .lastName(author.getLastName())
                    .build();
        }

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
