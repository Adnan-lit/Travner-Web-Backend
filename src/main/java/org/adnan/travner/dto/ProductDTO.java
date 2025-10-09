package org.adnan.travner.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;  // Fixed: Changed from Double to BigDecimal to match ProductEntry
    private String category;
    private List<String> images;
    private String sellerId;
    private String sellerUsername;
    private Integer stockQuantity;
    private Boolean isAvailable;
    private String location;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer views;
    private Double rating;
    private Integer reviewCount;
}