package org.adnan.travner.repository;

import org.adnan.travner.entry.UserEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;

public interface UserRepository extends MongoRepository<UserEntry, ObjectId> {
    UserEntry findByuserName(String userName);
    
    UserEntry findByEmail(String email);

    void deleteByuserName(String userName);

    @Query("{'$or': [{'userName': {'$regex': '?0', '$options': 'i'}}, {'firstName': {'$regex': '?0', '$options': 'i'}}, {'lastName': {'$regex': '?0', '$options': 'i'}}]}")
    Page<UserEntry> searchUsers(String query, Pageable pageable);
    
    // Analytics methods
    long countByLastLoginAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
}
