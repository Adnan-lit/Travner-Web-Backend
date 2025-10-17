package org.adnan.travner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating cart items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {
    
    @NotNull(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be 0 or positive")
    private Integer quantity;
}