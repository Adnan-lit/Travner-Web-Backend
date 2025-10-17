package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.entry.OrderEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private String id;
    private String orderNumber;
    private String userId;
    private String userEmail;
    private List<OrderItemDTO> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private OrderEntry.OrderStatus status;
    private ShippingAddressDTO shippingAddress;
    private PaymentInfoDTO paymentInfo;
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime paidAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private String productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private String sellerId;
        private String sellerName;
        private String productImage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressDTO {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoDTO {
        private String paymentMethod;
        private String transactionId;
        private OrderEntry.PaymentStatus paymentStatus;
        private LocalDateTime paidAt;
    }
}