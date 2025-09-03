package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private String id;
    private String content;
    private UserSummaryDTO author;
    private String postId;
    private LocalDateTime createdAt;
    private int upvotes;
    private int downvotes;
    private List<CommentDTO> replies;
    private String parentCommentId;
}
