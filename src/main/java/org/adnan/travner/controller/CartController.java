package org.adnan.travner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.AddToCartRequest;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.CartDTO;
import org.adnan.travner.dto.UpdateCartItemRequest;
import org.adnan.travner.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling shopping cart operations
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    /**
     * Get user's shopping cart
     * 
     * @param authentication User authentication
     * @return User's cart with all items
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CartDTO cart = cartService.getUserCart(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
        } catch (Exception e) {
            log.error("Error getting cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve cart: " + e.getMessage()));
        }
    }

    /**
     * Add item to cart
     * 
     * @param authentication User authentication
     * @param request        Add to cart request
     * @return Updated cart
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CartDTO cart = cartService.addToCart(authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
        } catch (RuntimeException e) {
            log.error("Error adding item to cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error adding item to cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add item to cart"));
        }
    }

    /**
     * Update cart item quantity
     * 
     * @param authentication User authentication
     * @param request        Update cart item request
     * @return Updated cart
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItem(
            Authentication authentication,
            @Valid @RequestBody UpdateCartItemRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CartDTO cart = cartService.updateCartItem(authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", cart));
        } catch (RuntimeException e) {
            log.error("Error updating cart item for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating cart item for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update cart item"));
        }
    }

    /**
     * Remove item from cart
     * 
     * @param authentication User authentication
     * @param productId      Product ID to remove
     * @return Updated cart
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeFromCart(
            Authentication authentication,
            @PathVariable String productId) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CartDTO cart = cartService.removeFromCart(authentication.getName(), productId);
            return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
        } catch (RuntimeException e) {
            log.error("Error removing item from cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error removing item from cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to remove item from cart"));
        }
    }

    /**
     * Clear entire cart
     * 
     * @param authentication User authentication
     * @return Empty cart
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartDTO>> clearCart(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            CartDTO cart = cartService.clearCart(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", cart));
        } catch (Exception e) {
            log.error("Error clearing cart for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to clear cart: " + e.getMessage()));
        }
    }

    /**
     * Get cart item count
     * 
     * @param authentication User authentication
     * @return Total number of items in cart
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            int itemCount = cartService.getCartItemCount(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Cart item count retrieved successfully", itemCount));
        } catch (Exception e) {
            log.error("Error getting cart item count for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get cart item count: " + e.getMessage()));
        }
    }
}