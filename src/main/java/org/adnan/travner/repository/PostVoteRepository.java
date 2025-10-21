package org.adnan.travner.repository;

import org.adnan.travner.entry.PostVoteEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface PostVoteRepository extends MongoRepository<PostVoteEntry, ObjectId> {

    Optional<PostVoteEntry> findByPostIdAndUserId(ObjectId postId, ObjectId userId);

    long countByPostIdAndVoteType(ObjectId postId, PostVoteEntry.VoteType voteType);

    List<PostVoteEntry> findByPostId(ObjectId postId);

    void deleteByPostIdAndUserId(ObjectId postId, ObjectId userId);
    
    // Analytics methods
    long countByUserId(ObjectId userId);
    long countByCreatedAtAfter(LocalDateTime date);
}
