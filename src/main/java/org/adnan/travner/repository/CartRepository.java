package org.adnan.travner.repository;

import org.adnan.travner.entry.CartEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Cart operations
 */
@Repository
public interface CartRepository extends MongoRepository<CartEntry, ObjectId> {

    /**
     * Find cart by user ID
     */
    Optional<CartEntry> findByUserId(String userId);

    /**
     * Check if cart exists for user
     */
    boolean existsByUserId(String userId);

    /**
     * Delete cart by user ID
     */
    void deleteByUserId(String userId);
}