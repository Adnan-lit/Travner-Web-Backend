package org.adnan.travner.repository;

import org.adnan.travner.entry.UserEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserEntry, ObjectId> {
    UserEntry findByuserName(String userName);

    void deleteByuserName(String userName);
}
