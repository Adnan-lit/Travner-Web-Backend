package org.adnan.travner.unit;

import org.adnan.travner.dto.AddToCartRequest;
import org.adnan.travner.dto.CartDTO;
import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.dto.UpdateCartItemRequest;
import org.adnan.travner.entry.CartEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DTOs and basic functionality (no Spring context required)
 */
class BasicFunctionalityTest {

    @Test
    void testAddToCartRequestCreation() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod123")
                .quantity(2)
                .build();

        assertEquals("prod123", request.getProductId());
        assertEquals(2, request.getQuantity());
    }

    @Test
    void testUpdateCartItemRequestCreation() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .productId("prod123")
                .quantity(5)
                .build();

        assertEquals("prod123", request.getProductId());
        assertEquals(5, request.getQuantity());
    }

    @Test
    void testCartEntryCreation() {
        CartEntry.CartItem item1 = CartEntry.CartItem.builder()
                .productId("prod1")
                .quantity(2)
                .build();

        CartEntry.CartItem item2 = CartEntry.CartItem.builder()
                .productId("prod2")
                .quantity(1)
                .build();

        CartEntry cart = CartEntry.builder()
                .userId("user123")
                .items(Arrays.asList(item1, item2))
                .build();

        assertEquals("user123", cart.getUserId());
        assertEquals(2, cart.getItems().size());
        assertEquals("prod1", cart.getItems().get(0).getProductId());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void testProductDTOCreation() {
        ProductDTO product = ProductDTO.builder()
                .id("prod123")
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("29.99"))
                .category("Electronics")
                .stockQuantity(100)
                .build();

        assertEquals("prod123", product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals("Test Description", product.getDescription());
        assertEquals(new BigDecimal("29.99"), product.getPrice());
        assertEquals("Electronics", product.getCategory());
        assertEquals(Integer.valueOf(100), product.getStockQuantity());
    }

    @Test
    void testCartItemDTOCreation() {
        CartDTO.CartItemDTO item = CartDTO.CartItemDTO.builder()
                .productId("prod123")
                .productName("Test Product")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(2)
                .subtotal(new BigDecimal("59.98"))
                .build();

        assertEquals("prod123", item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals(new BigDecimal("29.99"), item.getUnitPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("59.98"), item.getSubtotal());
    }

    @Test
    void testCartItemCalculations() {
        BigDecimal unitPrice = new BigDecimal("29.99");
        int quantity = 3;
        BigDecimal expectedSubtotal = unitPrice.multiply(new BigDecimal(quantity));

        CartDTO.CartItemDTO item = CartDTO.CartItemDTO.builder()
                .productId("prod123")
                .productName("Test Product")
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(expectedSubtotal)
                .build();

        assertEquals(new BigDecimal("89.97"), item.getSubtotal());
        assertTrue(item.getSubtotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testDTOValidation() {
        // Test that builder creates non-null objects
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("test")
                .quantity(1)
                .build();

        assertNotNull(request);
        assertNotNull(request.getProductId());
        assertTrue(request.getQuantity() > 0);
    }

    @Test
    void testCartItemEdgeCases() {
        // Test zero quantity
        CartDTO.CartItemDTO zeroQuantity = CartDTO.CartItemDTO.builder()
                .productId("prod1")
                .productName("Product 1")
                .unitPrice(new BigDecimal("10.00"))
                .quantity(0)
                .subtotal(BigDecimal.ZERO)
                .build();

        assertEquals(0, zeroQuantity.getQuantity());
        assertEquals(BigDecimal.ZERO, zeroQuantity.getSubtotal());

        // Test large quantities
        CartDTO.CartItemDTO largeQuantity = CartDTO.CartItemDTO.builder()
                .productId("prod2")
                .productName("Product 2")
                .unitPrice(new BigDecimal("5.99"))
                .quantity(1000)
                .subtotal(new BigDecimal("5990.00"))
                .build();

        assertEquals(1000, largeQuantity.getQuantity());
        assertEquals(new BigDecimal("5990.00"), largeQuantity.getSubtotal());
    }

    @Test
    void testProductDTOValidation() {
        ProductDTO product = ProductDTO.builder()
                .id("PROD-001")
                .name("Premium Product")
                .description("High quality product with excellent features")
                .price(new BigDecimal("199.99"))
                .category("Premium")
                .stockQuantity(25)
                .build();

        // Validate all fields are set correctly
        assertAll("product validation",
                () -> assertNotNull(product.getId()),
                () -> assertNotNull(product.getName()),
                () -> assertNotNull(product.getDescription()),
                () -> assertNotNull(product.getPrice()),
                () -> assertNotNull(product.getCategory()),
                () -> assertTrue(product.getStockQuantity() >= 0),
                () -> assertTrue(product.getPrice().compareTo(BigDecimal.ZERO) >= 0));
    }
}