package org.adnan.travner.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.adnan.travner.TravnerApplication;
import org.adnan.travner.dto.CartDTO;
import org.adnan.travner.entry.CartEntry;
import org.adnan.travner.repository.CartRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Cart functionality using embedded MongoDB
 */
@SpringBootTest(classes = TravnerApplication.class)
@ActiveProfiles("test")
@AutoConfigureWebMvc
class CartIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        // Clean up test data
        cartRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        org.adnan.travner.entry.UserEntry testUser = new org.adnan.travner.entry.UserEntry();
        testUser.setUserName(testUsername);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        userRepository.save(testUser);
    }

    @Test
    void contextLoads() {
        assertNotNull(cartService);
        assertNotNull(cartRepository);
    }

    @Test
    void testGetCartForUser() {
        // Test getting cart for user (should create if doesn't exist)
        CartDTO cart = cartService.getUserCart(testUsername);
        assertNotNull(cart);
        assertEquals(testUsername, cart.getUserId());
        assertNotNull(cart.getItems());
        assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
        assertEquals(0, cart.getTotalItems());
    }

    @Test
    void testCartPersistence() {
        // Create a cart entry
        CartEntry cartEntry = CartEntry.builder()
                .userId(testUsername)
                .items(new java.util.ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        cartRepository.save(cartEntry);

        // Verify it exists
        assertTrue(cartRepository.existsByUserId(testUsername));

        // Get the cart
        CartDTO cart = cartService.getUserCart(testUsername);
        assertNotNull(cart);
        assertEquals(testUsername, cart.getUserId());
    }

    @Test
    void testCartApiEndpoints() throws Exception {
        // Test GET cart endpoint with userId parameter (bypass authentication for testing)
        mockMvc.perform(get("/api/cart")
                .param("userId", testUsername)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Expect 401 since no authentication is provided

        // For now, we'll test that the endpoint exists and responds with 401
        // In a real application, you'd mock the authentication or use @WithMockUser
    }

    @Test
    void testCartServiceOperations() {
        // Test basic cart operations available in the service
        CartDTO cart = cartService.getUserCart(testUsername);
        assertNotNull(cart);

        // Test that cart is properly initialized
        assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
        assertEquals(0, cart.getTotalItems());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testCartDTOMapping() {
        // Test that CartDTO properly represents cart data
        CartDTO cart = CartDTO.builder()
                .userId(testUsername)
                .items(new java.util.ArrayList<>())
                .totalAmount(BigDecimal.valueOf(100.50))
                .totalItems(5)
                .build();

        assertEquals(testUsername, cart.getUserId());
        assertEquals(BigDecimal.valueOf(100.50), cart.getTotalAmount());
        assertEquals(5, cart.getTotalItems());
        assertNotNull(cart.getItems());
    }

    @Test
    void testCartItemDTO() {
        // Test CartItemDTO functionality
        CartDTO.CartItemDTO item = CartDTO.CartItemDTO.builder()
                .productId("prod123")
                .productName("Test Product")
                .unitPrice(BigDecimal.valueOf(29.99))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(59.98))
                .build();

        assertEquals("prod123", item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals(BigDecimal.valueOf(29.99), item.getUnitPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(BigDecimal.valueOf(59.98), item.getSubtotal());
    }
}