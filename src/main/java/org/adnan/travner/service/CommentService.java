package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.CommentDTO;
import org.adnan.travner.dto.CommentRequest;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.CommentEntry;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentDTO createComment(String postId, String username, CommentRequest commentRequest) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<PostEntry> postOptional = postRepository.findById(new ObjectId(postId));
        if (postOptional.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        ObjectId parentCommentId = null;
        if (commentRequest.getParentCommentId() != null && !commentRequest.getParentCommentId().isEmpty()) {
            parentCommentId = new ObjectId(commentRequest.getParentCommentId());
            Optional<CommentEntry> parentCommentOptional = commentRepository.findById(parentCommentId);
            if (parentCommentOptional.isEmpty()) {
                throw new RuntimeException("Parent comment not found");
            }
        }

        CommentEntry comment = CommentEntry.builder()
                .content(commentRequest.getContent())
                .author(user)
                .postId(new ObjectId(postId))
                .createdAt(LocalDateTime.now())
                .upvotes(0)
                .downvotes(0)
                .replies(new ArrayList<>())
                .parentCommentId(parentCommentId)
                .build();

        CommentEntry savedComment = commentRepository.save(comment);

        // If this is a reply to another comment, add this comment's ID to the parent's
        // replies list
        if (parentCommentId != null) {
            Optional<CommentEntry> parentCommentOptional = commentRepository.findById(parentCommentId);
            if (parentCommentOptional.isPresent()) {
                CommentEntry parentComment = parentCommentOptional.get();
                parentComment.getReplies().add(savedComment.getId());
                commentRepository.save(parentComment);
            }
        }

        return convertToDTO(savedComment, new ArrayList<>());
    }

    public Page<CommentDTO> getPostComments(String postId, Pageable pageable) {
        Page<CommentEntry> comments = commentRepository.findByPostIdAndParentCommentIdIsNull(
                new ObjectId(postId), pageable);

        return comments.map(comment -> {
            List<CommentEntry> replies = commentRepository.findByParentCommentId(comment.getId());
            List<CommentDTO> replyDTOs = replies.stream()
                    .map(reply -> convertToDTO(reply, new ArrayList<>()))
                    .collect(Collectors.toList());
            return convertToDTO(comment, replyDTOs);
        });
    }

    public Optional<CommentDTO> getCommentById(String id) {
        Optional<CommentEntry> commentOptional = commentRepository.findById(new ObjectId(id));
        return commentOptional.map(comment -> {
            List<CommentEntry> replies = commentRepository.findByParentCommentId(comment.getId());
            List<CommentDTO> replyDTOs = replies.stream()
                    .map(reply -> convertToDTO(reply, new ArrayList<>()))
                    .collect(Collectors.toList());
            return convertToDTO(comment, replyDTOs);
        });
    }

    public CommentDTO updateComment(String id, String username, CommentRequest commentRequest) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<CommentEntry> commentOptional = commentRepository.findById(new ObjectId(id));
        if (commentOptional.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        CommentEntry comment = commentOptional.get();

        // Check if the user is the author of the comment
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to update this comment");
        }

        comment.setContent(commentRequest.getContent());
        CommentEntry updatedComment = commentRepository.save(comment);

        List<CommentEntry> replies = commentRepository.findByParentCommentId(updatedComment.getId());
        List<CommentDTO> replyDTOs = replies.stream()
                .map(reply -> convertToDTO(reply, new ArrayList<>()))
                .collect(Collectors.toList());

        return convertToDTO(updatedComment, replyDTOs);
    }

    public void deleteComment(String id, String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<CommentEntry> commentOptional = commentRepository.findById(new ObjectId(id));
        if (commentOptional.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        CommentEntry comment = commentOptional.get();

        // Check if the user is the author or an admin
        if (!comment.getAuthor().getId().equals(user.getId()) &&
                (user.getRoles() == null || !user.getRoles().contains("ROLE_ADMIN"))) {
            throw new RuntimeException("You are not authorized to delete this comment");
        }

        // Delete all replies if it's a parent comment
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            for (ObjectId replyId : comment.getReplies()) {
                commentRepository.deleteById(replyId);
            }
        }

        // Remove this comment from parent's replies if it's a reply
        if (comment.getParentCommentId() != null) {
            Optional<CommentEntry> parentOptional = commentRepository.findById(comment.getParentCommentId());
            if (parentOptional.isPresent()) {
                CommentEntry parent = parentOptional.get();
                parent.getReplies().remove(comment.getId());
                commentRepository.save(parent);
            }
        }

        commentRepository.delete(comment);
    }

    public CommentDTO updateVote(String id, String username, boolean isUpvote) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<CommentEntry> commentOptional = commentRepository.findById(new ObjectId(id));
        if (commentOptional.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        CommentEntry comment = commentOptional.get();

        if (isUpvote) {
            comment.setUpvotes(comment.getUpvotes() + 1);
        } else {
            comment.setDownvotes(comment.getDownvotes() + 1);
        }

        CommentEntry updatedComment = commentRepository.save(comment);
        List<CommentEntry> replies = commentRepository.findByParentCommentId(updatedComment.getId());
        List<CommentDTO> replyDTOs = replies.stream()
                .map(reply -> convertToDTO(reply, new ArrayList<>()))
                .collect(Collectors.toList());

        return convertToDTO(updatedComment, replyDTOs);
    }

    private CommentDTO convertToDTO(CommentEntry comment, List<CommentDTO> replies) {
        UserEntry author = comment.getAuthor();
        UserSummaryDTO authorDTO = UserSummaryDTO.builder()
                .id(author.getId().toString())
                .userName(author.getUserName())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .build();

        return CommentDTO.builder()
                .id(comment.getId().toString())
                .content(comment.getContent())
                .author(authorDTO)
                .postId(comment.getPostId().toString())
                .createdAt(comment.getCreatedAt())
                .upvotes(comment.getUpvotes())
                .downvotes(comment.getDownvotes())
                .replies(replies)
                .parentCommentId(comment.getParentCommentId() != null ? comment.getParentCommentId().toString() : null)
                .build();
    }
}
