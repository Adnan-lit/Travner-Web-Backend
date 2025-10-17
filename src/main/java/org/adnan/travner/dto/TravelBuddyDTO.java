package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.entry.TravelBuddyEntry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for travel buddy data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelBuddyDTO {
    private String id;
    private UserSummaryDTO requester;
    private String destination;
    private String destinationCountry;
    private String destinationCity;
    private LocalDateTime travelDate;
    private LocalDateTime returnDate;
    private String description;
    private String travelPurpose;
    private TravelBuddyEntry.BuddyStatus status;
    private Integer maxAge;
    private Integer minAge;
    private String preferredGender;
    private List<String> interests;
    private String budgetRange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private List<String> interestedUserIds;
    private List<String> matchedUserIds;
}
