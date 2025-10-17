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
 * Request DTO for creating/updating itineraries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotBlank(message = "Destination is required")
    private String destination;
    
    private String destinationCountry;
    private String destinationCity;
    
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
    
    private List<ItineraryItemRequest> items;
    private List<String> tags;
    @Builder.Default
    private boolean isPublic = true;
    @Builder.Default
    private boolean isTemplate = false;
    private Integer estimatedBudget;
    @Builder.Default
    private String currency = "USD";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryItemRequest {
        @NotBlank(message = "Item title is required")
        private String title;
        
        private String description;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String category;
        private Double latitude;
        private Double longitude;
        private String address;
        private String notes;
        private Integer estimatedCost;
        private String currency;
        private List<String> attachments;
    }
}
