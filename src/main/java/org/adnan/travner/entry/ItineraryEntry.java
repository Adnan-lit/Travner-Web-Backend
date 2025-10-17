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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a travel itinerary
 */
@Document(collection = "itineraries")
@CompoundIndexes({
    @CompoundIndex(def = "{'author': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'destination': 1, 'startDate': 1}"),
    @CompoundIndex(def = "{'isPublic': 1, 'createdAt': -1}"),
    @CompoundIndex(def = "{'tags': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryEntry {

    @Id
    private ObjectId id;

    private String title;
    private String description;
    
    @Indexed
    private String destination;
    
    private String destinationCountry;
    private String destinationCity;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @DBRef
    @Indexed
    private UserEntry author;
    
    @Builder.Default
    private List<ItineraryItem> items = new ArrayList<>();
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Indexed
    @Builder.Default
    private boolean isPublic = true;
    
    @Indexed
    @Builder.Default
    private boolean isTemplate = false;
    
    private Integer estimatedBudget;
    private String currency;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private Integer likes = 0;
    @Builder.Default
    private Integer shares = 0;
    @Builder.Default
    private Integer views = 0;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryItem {
        private String title;
        private String description;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String category; // accommodation, transport, activity, food, etc.
        private Double latitude;
        private Double longitude;
        private String address;
        private String notes;
        private Integer estimatedCost;
        private String currency;
        private List<String> attachments; // URLs to photos, documents, etc.
    }
}
