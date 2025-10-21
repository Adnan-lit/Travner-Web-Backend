package org.adnan.travner.repository;

import org.adnan.travner.entry.ItineraryEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for itinerary operations
 */
@Repository
public interface ItineraryRepository extends MongoRepository<ItineraryEntry, ObjectId> {

    // Find public itineraries by destination
    Page<ItineraryEntry> findByDestinationContainingIgnoreCaseAndIsPublicTrue(String destination, Pageable pageable);
    
    // Find itineraries by author
    Page<ItineraryEntry> findByAuthor_Id(ObjectId authorId, Pageable pageable);
    
    // Find public itineraries by tags
    Page<ItineraryEntry> findByTagsInAndIsPublicTrue(List<String> tags, Pageable pageable);
    
    // Find itineraries by date range
    @Query("{'startDate': {$gte: ?0, $lte: ?1}, 'isPublic': true}")
    Page<ItineraryEntry> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find itineraries by destination and date range
    @Query("{'destination': {$regex: ?0, $options: 'i'}, 'startDate': {$gte: ?1, $lte: ?2}, 'isPublic': true}")
    Page<ItineraryEntry> findByDestinationAndDateRange(String destination, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find template itineraries
    Page<ItineraryEntry> findByIsTemplateTrueAndIsPublicTrue(Pageable pageable);
    
    // Search itineraries by text
    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}, {'destination': {$regex: ?0, $options: 'i'}}], 'isPublic': true}")
    Page<ItineraryEntry> searchItineraries(String query, Pageable pageable);
    
    // Find itineraries by country
    Page<ItineraryEntry> findByDestinationCountryContainingIgnoreCaseAndIsPublicTrue(String country, Pageable pageable);
    
    // Find itineraries by city
    Page<ItineraryEntry> findByDestinationCityContainingIgnoreCaseAndIsPublicTrue(String city, Pageable pageable);
    
    // Find most popular itineraries (by likes)
    Page<ItineraryEntry> findByIsPublicTrueOrderByLikesDesc(Pageable pageable);
    
    // Analytics methods
    long countByAuthor_Id(ObjectId authorId);
    List<ItineraryEntry> findByAuthor_IdAndCreatedAtAfter(ObjectId authorId, LocalDateTime date);
    List<ItineraryEntry> findByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
    List<ItineraryEntry> findTop10ByCreatedAtAfterOrderByViewsDesc(LocalDateTime date);
}
