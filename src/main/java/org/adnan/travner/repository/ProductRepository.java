package org.adnan.travner.repository;

import org.adnan.travner.entry.ProductEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ProductRepository extends MongoRepository<ProductEntry, ObjectId> {

    /**
     * Find all available products
     */
    Page<ProductEntry> findByIsAvailableTrue(Pageable pageable);

    /**
     * Find products by category
     */
    Page<ProductEntry> findByCategoryAndIsAvailableTrue(String category, Pageable pageable);

    /**
     * Find products by seller
     */
    Page<ProductEntry> findBySellerIdAndIsAvailableTrue(String sellerId, Pageable pageable);

    /**
     * Find products by location
     */
    Page<ProductEntry> findByLocationContainingIgnoreCaseAndIsAvailableTrue(String location, Pageable pageable);

    /**
     * Search products by name or description
     */
    @Query("{'$and': [" +
            "{'isAvailable': true}, " +
            "{'$or': [" +
            "{'name': {'$regex': ?0, '$options': 'i'}}, " +
            "{'description': {'$regex': ?0, '$options': 'i'}}, " +
            "{'tags': {'$regex': ?0, '$options': 'i'}}" +
            "]}" +
            "]}")
    Page<ProductEntry> searchProducts(String query, Pageable pageable);

    /**
     * Find products by tags
     */
    Page<ProductEntry> findByTagsInAndIsAvailableTrue(Collection<String> tags, Pageable pageable);

    /**
     * Count all available products
     */
    long countByIsAvailableTrue();
    
    // Analytics methods
    long countBySellerId(String sellerId);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfterAndIsAvailable(LocalDateTime date, Boolean isAvailable);
    List<ProductEntry> findByCreatedAtAfter(LocalDateTime date);

}