package org.adnan.travner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating/updating local guide profiles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalGuideRequest {
    
    @NotBlank(message = "Location is required")
    private String location;
    
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
    
    @NotNull(message = "Specialties are required")
    @Size(min = 1, message = "At least one specialty is required")
    private List<String> specialties;
    
    @NotNull(message = "Languages are required")
    @Size(min = 1, message = "At least one language is required")
    private List<String> languages;
    
    @NotNull(message = "Hourly rate is required")
    private BigDecimal hourlyRate;
    
    @Builder.Default
    private String currency = "USD";
    private String availability;
    private String contactMethod;
    private String contactInfo;
    private List<String> certifications;
    private List<String> experience;
    private List<String> services;
}
