package org.adnan.travner.repository;

import org.adnan.travner.entry.OrderEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Order operations
 */
@Repository
public interface OrderRepository extends MongoRepository<OrderEntry, ObjectId> {

    /**
     * Find all orders for a user
     */
    List<OrderEntry> findByUserIdOrderByOrderedAtDesc(String userId);

    /**
     * Find order by order number
     */
    Optional<OrderEntry> findByOrderNumber(String orderNumber);

    /**
     * Find order by ID and user ID
     */
    Optional<OrderEntry> findByIdAndUserId(ObjectId id, String userId);

    /**
     * Find orders by status for a user
     */
    List<OrderEntry> findByUserIdAndStatusOrderByOrderedAtDesc(String userId, OrderEntry.OrderStatus status);
}

