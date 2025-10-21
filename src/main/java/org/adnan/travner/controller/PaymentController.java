package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Payment Controller for handling Stripe payments
 */
//@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a payment intent for an order
     */
    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentIntent(
            @RequestParam String orderId,
            @RequestParam String customerEmail) {
        try {
            Map<String, Object> paymentIntent = paymentService.createPaymentIntent(orderId, customerEmail);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(paymentIntent)
                    .message("Payment intent created successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error creating payment intent for order: {}", orderId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to create payment intent: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Confirm a payment intent
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPaymentIntent(
            @RequestParam String paymentIntentId) {
        try {
            Map<String, Object> result = paymentService.confirmPaymentIntent(paymentIntentId);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(result)
                    .message("Payment confirmed successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error confirming payment intent: {}", paymentIntentId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to confirm payment: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Create a payment method
     */
    @PostMapping("/payment-method")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentMethod(
            @RequestParam String cardNumber,
            @RequestParam String expMonth,
            @RequestParam String expYear,
            @RequestParam String cvc) {
        try {
            Map<String, Object> paymentMethod = paymentService.createPaymentMethod(cardNumber, expMonth, expYear, cvc);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(paymentMethod)
                    .message("Payment method created successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error creating payment method", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to create payment method: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Process refund for an order
     */
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processRefund(
            @RequestParam String orderId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason) {
        try {
            Map<String, Object> refund = paymentService.processRefund(orderId, amount, reason);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(refund)
                    .message("Refund processed successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error processing refund for order: {}", orderId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to process refund: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get payment status for an order
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(@PathVariable String orderId) {
        try {
            Map<String, Object> status = paymentService.getPaymentStatus(orderId);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(status)
                    .message("Payment status retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting payment status for order: {}", orderId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get payment status: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get supported payment methods
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportedPaymentMethods() {
        try {
            Map<String, Object> methods = paymentService.getSupportedPaymentMethods();
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(methods)
                    .message("Supported payment methods retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting supported payment methods", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get supported payment methods")
                    .build()
            );
        }
    }

    /**
     * Calculate payment fees
     */
    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePaymentFees(@RequestParam BigDecimal amount) {
        try {
            Map<String, Object> fees = paymentService.calculatePaymentFees(amount);
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(fees)
                    .message("Payment fees calculated successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error calculating payment fees for amount: {}", amount, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to calculate payment fees")
                    .build()
            );
        }
    }

    /**
     * Handle Stripe webhooks
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            if (paymentService.validateWebhookSignature(payload, signature)) {
                // Parse the webhook event
                // For now, skip webhook processing due to Stripe API complexity
                // TODO: Implement proper webhook handling
                return ResponseEntity.ok("Webhook processing temporarily disabled");
            } else {
                return ResponseEntity.badRequest().body("Invalid webhook signature");
            }
        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }

    /**
     * Get payment configuration for frontend
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentConfig() {
        try {
            Map<String, Object> config = Map.of(
                "publishableKey", paymentService.getStripePublishableKey(),
                "supportedMethods", paymentService.getSupportedPaymentMethods(),
                "currency", "usd",
                "country", "US"
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .data(config)
                    .message("Payment configuration retrieved successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting payment configuration", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get payment configuration")
                    .build()
            );
        }
    }
}
