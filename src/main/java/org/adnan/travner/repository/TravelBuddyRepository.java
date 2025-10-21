package org.adnan.travner.repository;

import org.adnan.travner.entry.TravelBuddyEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for travel buddy operations
 */
@Repository
public interface TravelBuddyRepository extends MongoRepository<TravelBuddyEntry, ObjectId> {

    // Find active travel buddy requests by destination
    Page<TravelBuddyEntry> findByDestinationContainingIgnoreCaseAndStatus(String destination, TravelBuddyEntry.BuddyStatus status, Pageable pageable);
    
    // Find travel buddy requests by requester
    Page<TravelBuddyEntry> findByRequester_Id(ObjectId requesterId, Pageable pageable);
    
    // Find travel buddy requests by date range
    @Query("{'travelDate': {$gte: ?0, $lte: ?1}, 'status': 'ACTIVE'}")
    Page<TravelBuddyEntry> findByTravelDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find travel buddy requests by destination and date
    @Query("{'destination': {$regex: ?0, $options: 'i'}, 'travelDate': {$gte: ?1, $lte: ?2}, 'status': 'ACTIVE'}")
    Page<TravelBuddyEntry> findByDestinationAndDateRange(String destination, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find travel buddy requests by interests
    @Query("{'interests': {$in: ?0}, 'status': 'ACTIVE'}")
    Page<TravelBuddyEntry> findByInterests(List<String> interests, Pageable pageable);
    
    // Find travel buddy requests by purpose
    Page<TravelBuddyEntry> findByTravelPurposeAndStatus(String purpose, TravelBuddyEntry.BuddyStatus status, Pageable pageable);
    
    // Find expired requests
    @Query("{'expiresAt': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<TravelBuddyEntry> findExpiredRequests(LocalDateTime now);
    
    // Find requests where user is interested
    @Query("{'interestedUsers': ?0, 'status': 'ACTIVE'}")
    Page<TravelBuddyEntry> findByInterestedUser(ObjectId userId, Pageable pageable);
    
    // Find requests where user is matched
    @Query("{'matchedUsers': ?0}")
    Page<TravelBuddyEntry> findByMatchedUser(ObjectId userId, Pageable pageable);
    
    // Count active requests by destination
    long countByDestinationContainingIgnoreCaseAndStatus(String destination, TravelBuddyEntry.BuddyStatus status);
    
    // Find requests by country
    Page<TravelBuddyEntry> findByDestinationCountryContainingIgnoreCaseAndStatus(String country, TravelBuddyEntry.BuddyStatus status, Pageable pageable);
    
    // Find requests by city
    Page<TravelBuddyEntry> findByDestinationCityContainingIgnoreCaseAndStatus(String city, TravelBuddyEntry.BuddyStatus status, Pageable pageable);
    
    // Find by requester ID
    TravelBuddyEntry findByRequester_Id(ObjectId requesterId);
    
    // Find by status
    Page<TravelBuddyEntry> findByStatus(String status, Pageable pageable);
    
    // Find by interests and status
    Page<TravelBuddyEntry> findByInterestsInAndStatus(List<String> interests, String status, Pageable pageable);
    
    // Search buddies by text
    @Query("{'$or': [{'destination': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}, {'interests': {$regex: ?0, $options: 'i'}}], 'status': 'ACTIVE'}")
    Page<TravelBuddyEntry> searchBuddies(String query, Pageable pageable);
    
    // Analytics methods
    long countByRequester_Id(ObjectId requesterId);
    List<TravelBuddyEntry> findByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
    List<TravelBuddyEntry> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}
