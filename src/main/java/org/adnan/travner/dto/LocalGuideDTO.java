package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for local guide data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalGuideDTO {
    private String id;
    private UserSummaryDTO user;
    private String location;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private String bio;
    private List<String> specialties;
    private List<String> languages;
    private BigDecimal hourlyRate;
    private String currency;
    private boolean isAvailable;
    private String availability;
    private String contactMethod;
    private String contactInfo;
    private Double rating;
    private Integer reviewCount;
    private Integer totalBookings;
    private List<String> certifications;
    private List<String> experience;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActiveAt;
    private List<String> portfolio;
    private List<String> services;
}
