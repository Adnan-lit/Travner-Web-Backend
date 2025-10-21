package org.adnan.travner.repository;

import org.adnan.travner.entry.PostEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.time.LocalDateTime;
import org.bson.types.ObjectId;

public interface PostRepository extends MongoRepository<PostEntry, ObjectId> {

    Page<PostEntry> findByAuthor_Id(ObjectId authorId, Pageable pageable);

    @Query("{'tags': {$in: ?0}}")
    Page<PostEntry> findByTagsIn(List<String> tags, Pageable pageable);

    // Single method for finding published posts
    Page<PostEntry> findByPublishedTrue(Pageable pageable);

    @Query("{'location': {$regex: ?0, $options: 'i'}}")
    Page<PostEntry> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    Page<PostEntry> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("{$or: [{'title': {$regex: ?0, $options: 'i'}}, {'content': {$regex: ?0, $options: 'i'}}, {'location': {$regex: ?0, $options: 'i'}}, {'tags': {$regex: ?0, $options: 'i'}}]}")
    Page<PostEntry> searchPosts(String searchTerm, Pageable pageable);
    
    // Analytics methods
    long countByAuthor_Id(ObjectId authorId);
    long countByAuthorId(ObjectId authorId);
    List<PostEntry> findByAuthor_IdAndCreatedAtAfter(ObjectId authorId, LocalDateTime date);
    List<PostEntry> findByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
    List<PostEntry> findTop10ByCreatedAtAfterOrderByUpvotesDesc(LocalDateTime date);
}
