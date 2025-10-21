package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Follow relationship entry - represents a user following another user
 */
@Document(collection = "follows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "follower_following_idx", def = "{'followerId': 1, 'followingId': 1}", unique = true)
@CompoundIndex(name = "following_idx", def = "{'followingId': 1, 'createdAt': -1}")
@CompoundIndex(name = "follower_idx", def = "{'followerId': 1, 'createdAt': -1}")
public class FollowEntry {

    @Id
    private ObjectId id;

    /**
     * User ID who is following
     */
    private ObjectId followerId;

    /**
     * User ID being followed
     */
    private ObjectId followingId;

    /**
     * When the follow relationship was created
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}



