package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.AddToCartRequest;
import org.adnan.travner.dto.CartDTO;
import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.dto.UpdateCartItemRequest;
import org.adnan.travner.entry.CartEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CartRepository;
import org.adnan.travner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for handling cart operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    /**
     * Get user's cart or create a new one if it doesn't exist
     */
    public CartDTO getUserCart(String username) {
        log.debug("Getting cart for user: {}", username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        Optional<CartEntry> cartOpt = cartRepository.findByUserId(username); // Use username instead of ObjectId

        CartEntry cart;
        if (cartOpt.isPresent()) {
            cart = cartOpt.get();
        } else {
            // Create new cart for user
            cart = CartEntry.builder()
                    .userId(username) // Use username instead of ObjectId
                    .build();
            cart = cartRepository.save(cart);
            log.debug("Created new cart for user: {}", username);
        }

        return convertToDTO(cart);
    }

    /**
     * Add item to cart
     */
    @Transactional
    public CartDTO addToCart(String username, AddToCartRequest request) {
        log.debug("Adding product {} to cart for user: {}", request.getProductId(), username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Verify product exists and is available
        ProductDTO product = productService.getProductById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        if (!product.getIsAvailable()) {
            throw new RuntimeException("Product is not available for purchase");
        }

        // Check if product has enough stock
        if (product.getStockQuantity() != null && product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        // Get or create cart using username
        CartEntry cart = cartRepository.findByUserId(username)
                .orElse(CartEntry.builder()
                        .userId(username)
                        .build());

        // Create cart item
        CartEntry.CartItem cartItem = CartEntry.CartItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice()) // This should work - both are BigDecimal
                .quantity(request.getQuantity())
                .sellerId(product.getSellerId())
                .sellerName(product.getSellerUsername())
                .productImage(product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null)
                .addedAt(LocalDateTime.now())
                .build();

        // Calculate subtotal
        cartItem.calculateSubtotal();

        // Add item to cart
        cart.addItem(cartItem);

        // Save cart
        cart = cartRepository.save(cart);

        log.debug("Added {} units of product {} to cart for user: {}",
                request.getQuantity(), request.getProductId(), username);

        return convertToDTO(cart);
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public CartDTO updateCartItem(String username, UpdateCartItemRequest request) {
        log.debug("Updating cart item {} quantity to {} for user: {}",
                request.getProductId(), request.getQuantity(), username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        CartEntry cart = cartRepository.findByUserId(username)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + username));

        // Update item quantity
        boolean updated = cart.updateItemQuantity(request.getProductId(), request.getQuantity());

        if (!updated) {
            throw new RuntimeException("Product not found in cart: " + request.getProductId());
        }

        // Save cart
        cart = cartRepository.save(cart);

        log.debug("Updated cart item {} quantity to {} for user: {}",
                request.getProductId(), request.getQuantity(), username);

        return convertToDTO(cart);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public CartDTO removeFromCart(String username, String productId) {
        log.debug("Removing product {} from cart for user: {}", productId, username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        CartEntry cart = cartRepository.findByUserId(username)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + username));

        boolean removed = cart.removeItem(productId);

        if (!removed) {
            throw new RuntimeException("Product not found in cart: " + productId);
        }

        // Save cart
        cart = cartRepository.save(cart);

        log.debug("Removed product {} from cart for user: {}", productId, username);

        return convertToDTO(cart);
    }

    /**
     * Clear entire cart
     */
    @Transactional
    public CartDTO clearCart(String username) {
        log.debug("Clearing cart for user: {}", username);

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        CartEntry cart = cartRepository.findByUserId(username)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + username));

        cart.clearCart();

        // Save cart
        cart = cartRepository.save(cart);

        log.debug("Cleared cart for user: {}", username);

        return convertToDTO(cart);
    }

    /**
     * Get cart item count for user
     */
    public int getCartItemCount(String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        return cartRepository.findByUserId(username)
                .map(CartEntry::getTotalItems)
                .orElse(0);
    }

    /**
     * Convert CartEntry to CartDTO
     */
    private CartDTO convertToDTO(CartEntry cart) {
        return CartDTO.builder()
                .id(cart.getId().toString())
                .userId(cart.getUserId())
                .items(cart.getItems().stream()
                        .map(this::convertItemToDTO)
                        .collect(Collectors.toList()))
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getTotalItems())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * Convert CartItem to CartItemDTO
     */
    private CartDTO.CartItemDTO convertItemToDTO(CartEntry.CartItem item) {
        return CartDTO.CartItemDTO.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .sellerId(item.getSellerId())
                .sellerName(item.getSellerName())
                .productImage(item.getProductImage())
                .addedAt(item.getAddedAt())
                .build();
    }
}