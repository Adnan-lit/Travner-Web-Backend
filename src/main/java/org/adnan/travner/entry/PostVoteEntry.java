package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "post_votes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVoteEntry {

    @Id
    private ObjectId id;

    private ObjectId postId;
    private ObjectId userId;
    private VoteType voteType;
    private LocalDateTime createdAt;

    public enum VoteType {
        UPVOTE, DOWNVOTE
    }
}
