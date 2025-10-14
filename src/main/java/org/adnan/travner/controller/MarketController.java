package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.dto.ProductRequest;
import org.adnan.travner.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controller for handling market and product operations
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
public class MarketController {

    private final ProductService productService;

    /**
     * Get all available products with pagination and sorting
     * 
     * @param page      Zero-based page index (default 0)
     * @param size      Page size (default 10)
     * @param sortBy    Field to sort by (default createdAt)
     * @param direction Sort direction: asc or desc (default desc)
     * @return List of available products with pagination metadata
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

            // Make sure the field to sort by exists
            Pageable pageable;
            try {
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            } catch (Exception e) {
                // If the sort field is invalid, fall back to sorting by createdAt
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));
            }

            Page<ProductDTO> products = productService.getAllAvailableProducts(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve products: " + e.getMessage()));
        }
    }

    /**
     * Get a specific product by ID
     * 
     * @param id Product ID
     * @return Product details if found
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable String id) {
        try {
            Optional<ProductDTO> product = productService.getProductById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(product.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Product not found with ID: " + id));
            }
        } catch (Exception e) {
            log.error("Error retrieving product {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve product: " + e.getMessage()));
        }
    }

    /**
     * Search products by query term
     * 
     * @param query Search term
     * @param page  Zero-based page index (default 0)
     * @param size  Page size (default 10)
     * @return List of matching products with pagination metadata
     */
    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.searchProducts(query, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search products: " + e.getMessage()));
        }
    }

    /**
     * Get products by category
     * 
     * @param category Category name
     * @param page     Zero-based page index (default 0)
     * @param size     Page size (default 10)
     * @return List of products in the specified category with pagination metadata
     */
    @GetMapping("/products/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.getProductsByCategory(category, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products by category {}: {}", category, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get products by category: " + e.getMessage()));
        }
    }

    /**
     * Get products by location
     * 
     * @param location Location name (partial or full)
     * @param page     Zero-based page index (default 0)
     * @param size     Page size (default 10)
     * @return List of products from the specified location with pagination metadata
     */
    @GetMapping("/products/location")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByLocation(
            @RequestParam String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.getProductsByLocation(location, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products by location {}: {}", location, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get products by location: " + e.getMessage()));
        }
    }

    /**
     * Get products by tags
     * 
     * @param tags Comma-separated list of tags
     * @param page Zero-based page index (default 0)
     * @param size Page size (default 10)
     * @return List of products with the specified tags with pagination metadata
     */
    @GetMapping("/products/tags")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.getProductsByTags(tags, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products by tags {}: {}", tags, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get products by tags: " + e.getMessage()));
        }
    }

    /**
     * Get products by seller
     * 
     * @param sellerId Seller ID
     * @param page     Zero-based page index (default 0)
     * @param size     Page size (default 10)
     * @return List of products by the seller with pagination metadata
     */
    @GetMapping("/products/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsBySeller(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.getProductsBySeller(sellerId, pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products by seller {}: {}", sellerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve products by seller: " + e.getMessage()));
        }
    }

    /**
     * Create a new product
     * 
     * @param authentication User authentication
     * @param productRequest Product data
     * @return Created product details
     */
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequest productRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ProductDTO createdProduct = productService.createProduct(authentication.getName(), productRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Product created successfully", createdProduct));
        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create product: " + e.getMessage()));
        }
    }

    /**
     * Update an existing product
     * 
     * @param authentication User authentication
     * @param id Product ID
     * @param productRequest Updated product data
     * @return Updated product details
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ProductRequest productRequest) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            ProductDTO updatedProduct = productService.updateProduct(id, authentication.getName(), productRequest);
            return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
        } catch (RuntimeException e) {
            log.error("Error updating product {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to update product: " + e.getMessage()));
        }
    }

    /**
     * Delete a product
     * 
     * @param authentication User authentication
     * @param id Product ID
     * @return Success message
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteProduct(
            Authentication authentication,
            @PathVariable String id) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            productService.deleteProduct(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        } catch (RuntimeException e) {
            log.error("Error deleting product {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Failed to delete product: " + e.getMessage()));
        }
    }
}