package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.CartDTO;
import org.adnan.travner.dto.CreateOrderRequest;
import org.adnan.travner.dto.OrderDTO;
import org.adnan.travner.entry.OrderEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.OrderRepository;
import org.adnan.travner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling order operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    /**
     * Create order from cart
     */
    @Transactional
    public OrderDTO createOrder(String username, CreateOrderRequest request) {
        log.debug("Creating order for user: {}", username);

        // Get user
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Get cart
        CartDTO cart = cartService.getUserCart(username);
        if (cart.getTotalItems() == 0) {
            throw new RuntimeException("Cart is empty");
        }

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order items from cart items
        List<OrderEntry.OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderEntry.OrderItem.builder()
                        .productId(cartItem.getProductId())
                        .productName(cartItem.getProductName())
                        .unitPrice(cartItem.getUnitPrice())
                        .quantity(cartItem.getQuantity())
                        .subtotal(cartItem.getSubtotal())
                        .sellerId(cartItem.getSellerId())
                        .sellerName(cartItem.getSellerName())
                        .productImage(cartItem.getProductImage())
                        .build())
                .collect(Collectors.toList());

        // Create shipping address
        OrderEntry.ShippingAddress shippingAddress = OrderEntry.ShippingAddress.builder()
                .fullName(request.getShippingAddress().getFullName())
                .addressLine1(request.getShippingAddress().getAddressLine1())
                .addressLine2(request.getShippingAddress().getAddressLine2())
                .city(request.getShippingAddress().getCity())
                .state(request.getShippingAddress().getState())
                .zipCode(request.getShippingAddress().getZipCode())
                .country(request.getShippingAddress().getCountry())
                .phoneNumber(request.getShippingAddress().getPhoneNumber())
                .build();

        // Create payment info
        OrderEntry.PaymentInfo paymentInfo = OrderEntry.PaymentInfo.builder()
                .paymentMethod(request.getPaymentInfo().getPaymentMethod())
                .paymentStatus(OrderEntry.PaymentStatus.PENDING)
                .build();

        // Create order
        OrderEntry order = OrderEntry.builder()
                .orderNumber(orderNumber)
                .userId(username)
                .userEmail(user.getEmail())
                .items(orderItems)
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getTotalItems())
                .status(OrderEntry.OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .paymentInfo(paymentInfo)
                .orderedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        // Save order
        order = orderRepository.save(order);

        // Clear cart
        cartService.clearCart(username);

        log.info("Order created successfully: {} for user: {}", orderNumber, username);

        return convertToDTO(order);
    }

    /**
     * Get all orders for a user
     */
    public List<OrderDTO> getUserOrders(String username) {
        log.debug("Getting orders for user: {}", username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        List<OrderEntry> orders = orderRepository.findByUserIdOrderByOrderedAtDesc(username);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID
     */
    public OrderDTO getOrderById(String username, String orderId) {
        log.debug("Getting order {} for user: {}", orderId, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        OrderEntry order = orderRepository.findByIdAndUserId(new org.bson.types.ObjectId(orderId), username)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        return convertToDTO(order);
    }

    /**
     * Get order by order number
     */
    public OrderDTO getOrderByOrderNumber(String username, String orderNumber) {
        log.debug("Getting order {} for user: {}", orderNumber, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        OrderEntry order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        // Verify order belongs to user
        if (!order.getUserId().equals(username)) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        return convertToDTO(order);
    }

    /**
     * Cancel order
     */
    @Transactional
    public OrderDTO cancelOrder(String username, String orderId) {
        log.debug("Cancelling order {} for user: {}", orderId, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        OrderEntry order = orderRepository.findByIdAndUserId(new org.bson.types.ObjectId(orderId), username)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Only allow cancellation for PENDING and CONFIRMED orders
        if (order.getStatus() != OrderEntry.OrderStatus.PENDING
                && order.getStatus() != OrderEntry.OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderEntry.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        log.info("Order cancelled: {} for user: {}", orderId, username);

        return convertToDTO(order);
    }

    /**
     * Generate unique order number
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }

    /**
     * Convert OrderEntry to OrderDTO
     */
    private OrderDTO convertToDTO(OrderEntry order) {
        return OrderDTO.builder()
                .id(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .items(order.getItems().stream()
                        .map(this::convertItemToDTO)
                        .collect(Collectors.toList()))
                .totalAmount(order.getTotalAmount())
                .totalItems(order.getTotalItems())
                .status(order.getStatus())
                .shippingAddress(convertShippingAddressToDTO(order.getShippingAddress()))
                .paymentInfo(convertPaymentInfoToDTO(order.getPaymentInfo()))
                .orderedAt(order.getOrderedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .paidAt(order.getPaidAt())
                .fulfilledAt(order.getFulfilledAt())
                .cancelledAt(order.getCancelledAt())
                .cancelledBy(order.getCancelledBy())
                .notes(order.getNotes())
                .build();
    }

    private OrderDTO.OrderItemDTO convertItemToDTO(OrderEntry.OrderItem item) {
        return OrderDTO.OrderItemDTO.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .sellerId(item.getSellerId())
                .sellerName(item.getSellerName())
                .productImage(item.getProductImage())
                .build();
    }

    private OrderDTO.ShippingAddressDTO convertShippingAddressToDTO(OrderEntry.ShippingAddress address) {
        if (address == null) return null;
        return OrderDTO.ShippingAddressDTO.builder()
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .phoneNumber(address.getPhoneNumber())
                .build();
    }

    /**
     * Pay for an order
     */
    @Transactional
    public OrderDTO payOrder(String username, String orderId) {
        log.debug("Paying order {} for user: {}", orderId, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        OrderEntry order = orderRepository.findByIdAndUserId(new org.bson.types.ObjectId(orderId), username)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getStatus().equals(OrderEntry.OrderStatus.PENDING)) {
            throw new RuntimeException("Order cannot be paid. Current status: " + order.getStatus());
        }

        order.setStatus(OrderEntry.OrderStatus.PAID);
        order.setPaidAt(java.time.LocalDateTime.now());
        
        if (order.getPaymentInfo() != null) {
            order.getPaymentInfo().setPaymentStatus(OrderEntry.PaymentStatus.COMPLETED);
            order.getPaymentInfo().setPaidAt(java.time.LocalDateTime.now());
        }

        orderRepository.save(order);
        log.info("Order {} paid successfully by user: {}", orderId, username);

        return convertToDTO(order);
    }

    /**
     * Fulfill an order (seller action)
     */
    @Transactional
    public OrderDTO fulfillOrder(String username, String orderId) {
        log.debug("Fulfilling order {} for user: {}", orderId, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        OrderEntry order = orderRepository.findById(new org.bson.types.ObjectId(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getStatus().equals(OrderEntry.OrderStatus.PAID)) {
            throw new RuntimeException("Order cannot be fulfilled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderEntry.OrderStatus.FULFILLED);
        order.setFulfilledAt(java.time.LocalDateTime.now());
        orderRepository.save(order);
        
        log.info("Order {} fulfilled successfully by user: {}", orderId, username);
        return convertToDTO(order);
    }

    /**
     * Get all orders (admin only)
     */
    public List<OrderDTO> getAllOrders() {
        log.debug("Getting all orders");

        List<OrderEntry> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Admin cancel order
     */
    @Transactional
    public OrderDTO adminCancelOrder(String adminUsername, String orderId) {
        log.debug("Admin cancelling order {} by user: {}", orderId, adminUsername);

        UserEntry admin = userRepository.findByuserName(adminUsername);
        if (admin == null) {
            throw new RuntimeException("Admin user not found: " + adminUsername);
        }

        // Check if user is admin
        if (admin.getRoles() == null || !admin.getRoles().contains("ADMIN")) {
            throw new RuntimeException("Only administrators can cancel orders");
        }

        OrderEntry order = orderRepository.findById(new org.bson.types.ObjectId(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus().equals(OrderEntry.OrderStatus.CANCELLED)) {
            throw new RuntimeException("Order is already cancelled");
        }

        if (order.getStatus().equals(OrderEntry.OrderStatus.FULFILLED) || order.getStatus().equals(OrderEntry.OrderStatus.DELIVERED)) {
            throw new RuntimeException("Cannot cancel fulfilled or delivered order");
        }

        order.setStatus(OrderEntry.OrderStatus.CANCELLED);
        order.setCancelledAt(java.time.LocalDateTime.now());
        order.setCancelledBy(adminUsername);
        orderRepository.save(order);
        
        log.info("Order {} cancelled by admin: {}", orderId, adminUsername);
        return convertToDTO(order);
    }

    private OrderDTO.PaymentInfoDTO convertPaymentInfoToDTO(OrderEntry.PaymentInfo paymentInfo) {
        if (paymentInfo == null) return null;
        return OrderDTO.PaymentInfoDTO.builder()
                .paymentMethod(paymentInfo.getPaymentMethod())
                .transactionId(paymentInfo.getTransactionId())
                .paymentStatus(paymentInfo.getPaymentStatus())
                .paidAt(paymentInfo.getPaidAt())
                .build();
    }
}
