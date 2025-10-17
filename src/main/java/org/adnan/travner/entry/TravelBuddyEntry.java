package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a travel buddy match request
 */
@Document(collection = "travel_buddies")
@CompoundIndexes({
    @CompoundIndex(def = "{'requester': 1, 'status': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'destination': 1, 'travelDate': 1, 'status': 1}"),
    @CompoundIndex(def = "{'status': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelBuddyEntry {

    @Id
    private ObjectId id;

    @DBRef
    @Indexed
    private UserEntry requester;
    
    @Indexed
    private String destination;
    
    private String destinationCountry;
    private String destinationCity;
    
    private LocalDateTime travelDate;
    private LocalDateTime returnDate;
    
    private String description;
    private String travelPurpose; // business, leisure, adventure, etc.
    
    @Indexed
    private BuddyStatus status;
    
    private Integer maxAge;
    private Integer minAge;
    private String preferredGender;
    private List<String> interests;
    private String budgetRange;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    @Builder.Default
    private List<ObjectId> interestedUsers = new java.util.ArrayList<>();
    
    @Builder.Default
    private List<ObjectId> matchedUsers = new java.util.ArrayList<>();
    
    public enum BuddyStatus {
        ACTIVE,
        MATCHED,
        EXPIRED,
        CANCELLED
    }
}
