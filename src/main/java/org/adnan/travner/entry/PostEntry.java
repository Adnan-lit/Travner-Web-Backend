package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntry {

    @Id
    private ObjectId id;

    private String title;

    private String content;

    private String location;

    private List<String> mediaUrls; // URLs to images or videos stored in cloud storage

    @DBRef
    private UserEntry author;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private List<String> tags;

    private int upvotes;

    private int downvotes;

    @Builder.Default
    private List<ObjectId> comments = new ArrayList<>();

    // Changed from isPublished to published to avoid MongoDB document field name
    // issues
    private boolean published;
}
