package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@CompoundIndexes({
    @CompoundIndex(def = "{'author': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'published': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'location': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'tags': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntry {

    @Id
    private ObjectId id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    @Indexed
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private List<String> mediaUrls; // URLs to images or videos stored in cloud storage

    @DBRef
    @Indexed
    @NotNull(message = "Author is required")
    private UserEntry author;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed
    private List<String> tags;

    private int upvotes;

    private int downvotes;

    @Builder.Default
    private List<ObjectId> comments = new ArrayList<>();

    // Changed from isPublished to published to avoid MongoDB document field name
    // issues
    @Indexed
    private boolean published;
}
