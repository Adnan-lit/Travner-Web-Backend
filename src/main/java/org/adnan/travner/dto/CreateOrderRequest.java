package org.adnan.travner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request DTO for creating orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressRequest shippingAddress;
    
    @Valid
    @NotNull(message = "Payment info is required")
    private PaymentInfoRequest paymentInfo;
    
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;
        
        @NotBlank(message = "Address line 1 is required")
        private String addressLine1;
        
        private String addressLine2;
        
        @NotBlank(message = "City is required")
        private String city;
        
        @NotBlank(message = "State is required")
        private String state;
        
        @NotBlank(message = "Zip code is required")
        private String zipCode;
        
        @NotBlank(message = "Country is required")
        private String country;
        
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoRequest {
        @NotBlank(message = "Payment method is required")
        private String paymentMethod;
        
        private String transactionId;
    }
}