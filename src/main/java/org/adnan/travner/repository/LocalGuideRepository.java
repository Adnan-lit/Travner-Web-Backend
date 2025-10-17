package org.adnan.travner.repository;

import org.adnan.travner.entry.LocalGuideEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for local guide operations
 */
@Repository
public interface LocalGuideRepository extends MongoRepository<LocalGuideEntry, ObjectId> {

    // Find guides by location
    Page<LocalGuideEntry> findByLocationContainingIgnoreCaseAndIsAvailableTrue(String location, Pageable pageable);
    
    // Find guides by city
    Page<LocalGuideEntry> findByCityContainingIgnoreCaseAndIsAvailableTrue(String city, Pageable pageable);
    
    // Find guides by country
    Page<LocalGuideEntry> findByCountryContainingIgnoreCaseAndIsAvailableTrue(String country, Pageable pageable);
    
    // Find guides by specialties
    Page<LocalGuideEntry> findBySpecialtiesInAndIsAvailableTrue(List<String> specialties, Pageable pageable);
    
    // Find guides by languages
    Page<LocalGuideEntry> findByLanguagesInAndIsAvailableTrue(List<String> languages, Pageable pageable);
    
    // Find guides by user
    LocalGuideEntry findByUser_Id(ObjectId userId);
    
    // Find guides by rating range
    @Query("{'rating': {$gte: ?0, $lte: ?1}, 'isAvailable': true}")
    Page<LocalGuideEntry> findByRatingRange(double minRating, double maxRating, Pageable pageable);
    
    // Find guides by price range
    @Query("{'hourlyRate': {$gte: ?0, $lte: ?1}, 'isAvailable': true}")
    Page<LocalGuideEntry> findByPriceRange(double minPrice, double maxPrice, Pageable pageable);
    
    // Search guides by text
    @Query("{'$or': [{'bio': {$regex: ?0, $options: 'i'}}, {'specialties': {$regex: ?0, $options: 'i'}}, {'location': {$regex: ?0, $options: 'i'}}], 'isAvailable': true}")
    Page<LocalGuideEntry> searchGuides(String query, Pageable pageable);
    
    // Find top-rated guides
    Page<LocalGuideEntry> findByIsAvailableTrueOrderByRatingDesc(Pageable pageable);
    
    // Find guides by multiple criteria
    @Query("{'location': {$regex: ?0, $options: 'i'}, 'specialties': {$in: ?1}, 'languages': {$in: ?2}, 'isAvailable': true}")
    Page<LocalGuideEntry> findByLocationAndSpecialtiesAndLanguages(String location, List<String> specialties, List<String> languages, Pageable pageable);
    
    // Count guides by location
    long countByLocationContainingIgnoreCaseAndIsAvailableTrue(String location);
}
