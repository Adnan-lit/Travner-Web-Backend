package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for product data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
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