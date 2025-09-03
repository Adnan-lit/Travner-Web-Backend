package org.adnan.travner.repository;

import org.adnan.travner.entry.CommentEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<CommentEntry, ObjectId> {

    Page<CommentEntry> findByPostIdAndParentCommentIdIsNull(ObjectId postId, Pageable pageable);

    List<CommentEntry> findByParentCommentId(ObjectId parentCommentId);

    Page<CommentEntry> findByAuthor_Id(ObjectId authorId, Pageable pageable);

    long countByPostId(ObjectId postId);
}
