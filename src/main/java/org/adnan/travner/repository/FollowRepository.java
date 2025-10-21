package org.adnan.travner.repository;

import org.adnan.travner.entry.FollowEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing follow relationships
 */
@Repository
public interface FollowRepository extends MongoRepository<FollowEntry, ObjectId> {

    /**
     * Find follow relationship between two users
     */
    Optional<FollowEntry> findByFollowerIdAndFollowingId(ObjectId followerId, ObjectId followingId);

    /**
     * Check if follower follows following
     */
    boolean existsByFollowerIdAndFollowingId(ObjectId followerId, ObjectId followingId);

    /**
     * Get all users that the specified user follows
     */
    Page<FollowEntry> findByFollowerId(ObjectId followerId, Pageable pageable);

    /**
     * Get all users that follow the specified user
     */
    Page<FollowEntry> findByFollowingId(ObjectId followingId, Pageable pageable);

    /**
     * Count followers
     */
    long countByFollowingId(ObjectId followingId);

    /**
     * Count following
     */
    long countByFollowerId(ObjectId followerId);

    /**
     * Delete follow relationship
     */
    void deleteByFollowerIdAndFollowingId(ObjectId followerId, ObjectId followingId);
}


