package org.adnan.travner.repository;

import org.adnan.travner.entry.MediaEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MediaRepository extends MongoRepository<MediaEntry, ObjectId> {

    List<MediaEntry> findByPostId(ObjectId postId);

    List<MediaEntry> findByUploader_Id(ObjectId uploaderId);

    void deleteByPostId(ObjectId postId);

    List<MediaEntry> findByEntityId(String entityId);

    List<MediaEntry> findByEntityIdAndType(String entityId, String type);
}
