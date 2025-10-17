package org.adnan.travner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating travel buddy requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelBuddyRequest {
    
    @NotBlank(message = "Destination is required")
    private String destination;
    
    private String destinationCountry;
    private String destinationCity;
    
    @NotNull(message = "Travel date is required")
    private LocalDateTime travelDate;
    
    private LocalDateTime returnDate;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String travelPurpose;
    private Integer maxAge;
    private Integer minAge;
    private String preferredGender;
    private List<String> interests;
    private String budgetRange;
    @Builder.Default
    private Integer expiryDays = 30; // Default expiry in days
}
