package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user's shopping cart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class CartEntry {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Nested class representing an item in the cart
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private String productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private String sellerId;
        private String sellerName;
        private String productImage;
        private LocalDateTime addedAt;

        /**
         * Calculate subtotal based on unit price and quantity
         */
        public void calculateSubtotal() {
            if (unitPrice != null && quantity != null) {
                this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            }
        }
    }

    /**
     * Calculate total amount from all cart items
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .filter(item -> item.getSubtotal() != null)
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add item to cart
     */
    public void addItem(CartItem item) {
        // Check if item already exists in cart
        for (int i = 0; i < items.size(); i++) {
            CartItem existingItem = items.get(i);
            if (existingItem.getProductId().equals(item.getProductId())) {
                // Update quantity and recalculate subtotal
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                existingItem.calculateSubtotal();
                calculateTotalAmount();
                return;
            }
        }
        // Item doesn't exist, add new item
        items.add(item);
        calculateTotalAmount();
    }

    /**
     * Update item quantity
     */
    public boolean updateItemQuantity(String productId, Integer newQuantity) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                if (newQuantity <= 0) {
                    items.remove(item);
                } else {
                    item.setQuantity(newQuantity);
                    item.calculateSubtotal();
                }
                calculateTotalAmount();
                return true;
            }
        }
        return false;
    }

    /**
     * Remove item from cart
     */
    public boolean removeItem(String productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (removed) {
            calculateTotalAmount();
        }
        return removed;
    }

    /**
     * Clear all items from cart
     */
    public void clearCart() {
        items.clear();
        totalAmount = BigDecimal.ZERO;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get total number of items in cart
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}