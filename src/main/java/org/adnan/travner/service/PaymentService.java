package org.adnan.travner.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.OrderEntry;
import org.adnan.travner.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.bson.types.ObjectId;

/**
 * Payment Service for handling Stripe payments
 */
//@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${app.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    /**
     * Create a payment intent for an order
     */
    public Map<String, Object> createPaymentIntent(String orderId, String customerEmail) {
        try {
            Stripe.apiKey = stripeSecretKey;

            OrderEntry order = orderRepository.findById(new ObjectId(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd") // You can make this configurable
                .putMetadata("customerEmail", customerEmail)
                .putMetadata("orderId", orderId)
                .putMetadata("orderNumber", order.getOrderNumber())
                .setDescription("Payment for order " + order.getOrderNumber())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("amount", order.getTotalAmount());
            response.put("currency", "usd");

            return response;

        } catch (StripeException e) {
            log.error("Error creating payment intent for order: {}", orderId, e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Confirm a payment intent
     */
    public Map<String, Object> confirmPaymentIntent(String paymentIntentId) {
        try {
            Stripe.apiKey = stripeSecretKey;

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent confirmedPaymentIntent = paymentIntent.confirm();

            Map<String, Object> response = new HashMap<>();
            response.put("status", confirmedPaymentIntent.getStatus());
            response.put("paymentIntentId", confirmedPaymentIntent.getId());
            response.put("amount", confirmedPaymentIntent.getAmount());
            response.put("currency", confirmedPaymentIntent.getCurrency());

            // Update order status based on payment status
            if ("succeeded".equals(confirmedPaymentIntent.getStatus())) {
                updateOrderStatus(confirmedPaymentIntent.getMetadata().get("orderId"), "PAID");
            }

            return response;

        } catch (StripeException e) {
            log.error("Error confirming payment intent: {}", paymentIntentId, e);
            throw new RuntimeException("Failed to confirm payment: " + e.getMessage());
        }
    }

    /**
     * Create a payment method
     */
    public Map<String, Object> createPaymentMethod(String cardNumber, String expMonth, String expYear, String cvc) {
        try {
            Stripe.apiKey = stripeSecretKey;

            PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(
                    PaymentMethodCreateParams.CardDetails.builder()
                        .setNumber(cardNumber)
                        .setExpMonth(Long.parseLong(expMonth))
                        .setExpYear(Long.parseLong(expYear))
                        .setCvc(cvc)
                        .build()
                )
                .build();

            PaymentMethod paymentMethod = PaymentMethod.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentMethodId", paymentMethod.getId());
            response.put("type", paymentMethod.getType());
            response.put("card", paymentMethod.getCard());

            return response;

        } catch (StripeException e) {
            log.error("Error creating payment method", e);
            throw new RuntimeException("Failed to create payment method: " + e.getMessage());
        }
    }

    /**
     * Process refund for an order
     */
    public Map<String, Object> processRefund(String orderId, BigDecimal amount, String reason) {
        try {
            Stripe.apiKey = stripeSecretKey;

            OrderEntry order = orderRepository.findById(new ObjectId(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

            // Get the payment intent from the order
            String paymentIntentId = order.getPaymentInfo().getTransactionId();
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Create refund
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(
                com.stripe.param.RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue())
                    .setReason(com.stripe.param.RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .setMetadata(Map.of(
                        "orderId", orderId,
                        "reason", reason
                    ))
                    .build()
            );

            // Update order status
            updateOrderStatus(orderId, "REFUNDED");

            Map<String, Object> response = new HashMap<>();
            response.put("refundId", refund.getId());
            response.put("status", refund.getStatus());
            response.put("amount", refund.getAmount());
            response.put("currency", refund.getCurrency());

            return response;

        } catch (StripeException e) {
            log.error("Error processing refund for order: {}", orderId, e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Get payment status for an order
     */
    public Map<String, Object> getPaymentStatus(String orderId) {
        try {
            OrderEntry order = orderRepository.findById(new ObjectId(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

            if (order.getPaymentInfo() == null || order.getPaymentInfo().getTransactionId() == null) {
                return Map.of("status", "no_payment");
            }

            Stripe.apiKey = stripeSecretKey;
            PaymentIntent paymentIntent = PaymentIntent.retrieve(order.getPaymentInfo().getTransactionId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", paymentIntent.getStatus());
            response.put("amount", paymentIntent.getAmount());
            response.put("currency", paymentIntent.getCurrency());
            response.put("paymentMethod", paymentIntent.getPaymentMethod());
            response.put("created", paymentIntent.getCreated());

            return response;

        } catch (StripeException e) {
            log.error("Error getting payment status for order: {}", orderId, e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage());
        }
    }

    /**
     * Get supported payment methods
     */
    public Map<String, Object> getSupportedPaymentMethods() {
        Map<String, Object> methods = new HashMap<>();
        methods.put("card", Map.of(
            "name", "Credit/Debit Card",
            "enabled", true,
            "types", new String[]{"visa", "mastercard", "amex", "discover"}
        ));
        methods.put("digital_wallet", Map.of(
            "name", "Digital Wallet",
            "enabled", true,
            "types", new String[]{"apple_pay", "google_pay"}
        ));
        methods.put("bank_transfer", Map.of(
            "name", "Bank Transfer",
            "enabled", false,
            "types", new String[]{}
        ));
        
        return methods;
    }

    /**
     * Calculate payment fees
     */
    public Map<String, Object> calculatePaymentFees(BigDecimal amount) {
        // Stripe fees: 2.9% + $0.30 per transaction
        BigDecimal stripeFee = amount.multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
        BigDecimal totalAmount = amount.add(stripeFee);
        
        Map<String, Object> fees = new HashMap<>();
        fees.put("originalAmount", amount);
        fees.put("stripeFee", stripeFee);
        fees.put("totalAmount", totalAmount);
        fees.put("feePercentage", "2.9%");
        fees.put("fixedFee", "0.30");
        
        return fees;
    }

    private void updateOrderStatus(String orderId, String status) {
        try {
            OrderEntry order = orderRepository.findById(new ObjectId(orderId)).orElse(null);
            if (order != null) {
                order.setStatus(OrderEntry.OrderStatus.valueOf(status));
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("Error updating order status for order: {}", orderId, e);
        }
    }

    /**
     * Validate webhook signature
     */
    public boolean validateWebhookSignature(String payload, String signature) {
        try {
            com.stripe.net.Webhook.constructEvent(payload, signature, stripeWebhookSecret);
            return true;
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Handle webhook events
     */
    public void handleWebhookEvent(com.stripe.model.Event event) {
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                case "charge.dispute.created":
                    handleChargeDispute(event);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error handling webhook event: {}", event.getType(), e);
        }
    }

    private void handlePaymentSucceeded(com.stripe.model.Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent != null) {
            String orderId = paymentIntent.getMetadata().get("orderId");
            if (orderId != null) {
                updateOrderStatus(orderId, "PAID");
                log.info("Payment succeeded for order: {}", orderId);
            }
        }
    }

    private void handlePaymentFailed(com.stripe.model.Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent != null) {
            String orderId = paymentIntent.getMetadata().get("orderId");
            if (orderId != null) {
                updateOrderStatus(orderId, "PAYMENT_FAILED");
                log.info("Payment failed for order: {}", orderId);
            }
        }
    }

    private void handleChargeDispute(com.stripe.model.Event event) {
        com.stripe.model.Dispute dispute = (com.stripe.model.Dispute) event.getDataObjectDeserializer().getObject().orElse(null);
        if (dispute != null) {
            log.info("Charge dispute created: {}", dispute.getId());
            // Handle dispute logic here
        }
    }

    /**
     * Get Stripe publishable key
     */
    public String getStripePublishableKey() {
        return stripePublishableKey;
    }
}
