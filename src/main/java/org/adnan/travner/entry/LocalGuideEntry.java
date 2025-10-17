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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a local guide profile
 */
@Document(collection = "local_guides")
@CompoundIndexes({
    @CompoundIndex(def = "{'location': 1, 'isAvailable': 1, 'rating': -1}"),
    @CompoundIndex(def = "{'specialties': 1, 'isAvailable': 1, 'rating': -1}"),
    @CompoundIndex(def = "{'languages': 1, 'isAvailable': 1, 'rating': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalGuideEntry {

    @Id
    private ObjectId id;

    @DBRef
    @Indexed
    private UserEntry user;
    
    @Indexed
    private String location;
    
    private String city;
    private String country;
    
    private Double latitude;
    private Double longitude;
    
    private String bio;
    private List<String> specialties; // historical, food, adventure, cultural, etc.
    private List<String> languages;
    
    private BigDecimal hourlyRate;
    private String currency;
    
    @Indexed
    @Builder.Default
    private boolean isAvailable = true;
    
    private String availability; // "Weekdays 9-5", "Weekends only", etc.
    private String contactMethod; // phone, email, whatsapp, etc.
    private String contactInfo;
    
    @Builder.Default
    private Double rating = 0.0;
    @Builder.Default
    private Integer reviewCount = 0;
    @Builder.Default
    private Integer totalBookings = 0;
    
    private List<String> certifications;
    private List<String> experience; // years of experience, notable achievements
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastActiveAt;
    
    @Builder.Default
    private List<String> portfolio = new java.util.ArrayList<>(); // URLs to photos, videos, etc.
    
    @Builder.Default
    private List<String> services = new java.util.ArrayList<>(); // walking tours, food tours, etc.
}
