package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntry {

    @Id
    private ObjectId id;

    private String content;

    @DBRef
    private UserEntry author;

    private ObjectId postId;

    @CreatedDate
    private LocalDateTime createdAt;

    private int upvotes;

    private int downvotes;

    @Builder.Default
    private List<ObjectId> replies = new ArrayList<>();

    private ObjectId parentCommentId; // null if top-level comment
}
