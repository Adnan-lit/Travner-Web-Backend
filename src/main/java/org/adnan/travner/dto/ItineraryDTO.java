package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for itinerary data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDTO {
    private String id;
    private String title;
    private String description;
    private String destination;
    private String destinationCountry;
    private String destinationCity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UserSummaryDTO author;
    private List<ItineraryItemDTO> items;
    private List<String> tags;
    private boolean isPublic;
    private boolean isTemplate;
    private Integer estimatedBudget;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likes;
    private Integer shares;
    private Integer views;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryItemDTO {
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
