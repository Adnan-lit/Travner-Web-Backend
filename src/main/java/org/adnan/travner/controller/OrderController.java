package org.adnan.travner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.CreateOrderRequest;
import org.adnan.travner.dto.OrderDTO;
import org.adnan.travner.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling order operations
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/market/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Create order from cart
     *
     * @param authentication User authentication
     * @param request Order creation request with shipping and payment details
     * @return Created order
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            OrderDTO order = orderService.createOrder(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Order created successfully", order));
        } catch (RuntimeException e) {
            log.error("Error creating order for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating order for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create order"));
        }
    }

    /**
     * Get all orders for the authenticated user
     *
     * @param authentication User authentication
     * @return List of user's orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getUserOrders(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            List<OrderDTO> orders = orderService.getUserOrders(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            log.error("Error getting orders for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve orders: " + e.getMessage()));
        }
    }

    /**
     * Get order by ID
     *
     * @param authentication User authentication
     * @param orderId Order ID
     * @return Order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            Authentication authentication,
            @PathVariable String orderId) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            OrderDTO order = orderService.getOrderById(authentication.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
        } catch (RuntimeException e) {
            log.error("Error getting order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve order"));
        }
    }

    /**
     * Get order by order number
     *
     * @param authentication User authentication
     * @param orderNumber Order number
     * @return Order details
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByOrderNumber(
            Authentication authentication,
            @PathVariable String orderNumber) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            OrderDTO order = orderService.getOrderByOrderNumber(authentication.getName(), orderNumber);
            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
        } catch (RuntimeException e) {
            log.error("Error getting order {} for user: {}", orderNumber, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting order {} for user: {}", orderNumber, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve order"));
        }
    }

    /**
     * Cancel order
     *
     * @param authentication User authentication
     * @param orderId Order ID to cancel
     * @return Updated order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            Authentication authentication,
            @PathVariable String orderId) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            OrderDTO order = orderService.cancelOrder(authentication.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
        } catch (RuntimeException e) {
            log.error("Error cancelling order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cancel order"));
        }
    }


    /**
     * Pay for an order
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<OrderDTO>> payOrder(
            Authentication authentication,
            @PathVariable String orderId) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            OrderDTO order = orderService.payOrder(authentication.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.success("Order paid successfully", order));
        } catch (Exception e) {
            log.error("Error paying order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to pay order"));
        }
    }

    /**
     * Fulfill an order (seller action)
     */
    @PostMapping("/{orderId}/fulfill")
    public ResponseEntity<ApiResponse<OrderDTO>> fulfillOrder(
            Authentication authentication,
            @PathVariable String orderId) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            OrderDTO order = orderService.fulfillOrder(authentication.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.success("Order fulfilled successfully", order));
        } catch (Exception e) {
            log.error("Error fulfilling order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fulfill order"));
        }
    }

    /**
     * Get all orders (admin only)
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders(
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            List<OrderDTO> orders = orderService.getAllOrders();
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            log.error("Error retrieving all orders for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve orders"));
        }
    }

    /**
     * Admin cancel order
     */
    @PostMapping("/{orderId}/admin-cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> adminCancelOrder(
            Authentication authentication,
            @PathVariable String orderId) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            OrderDTO order = orderService.adminCancelOrder(authentication.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.success("Order cancelled by admin successfully", order));
        } catch (Exception e) {
            log.error("Error admin cancelling order {} for user: {}", orderId, authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cancel order"));
        }
    }
}
