package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.dto.ProductRequest;
import org.adnan.travner.entry.ProductEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.ProductRepository;
import org.adnan.travner.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get all available products with pagination
     */
    public Page<ProductDTO> getAllAvailableProducts(Pageable pageable) {
        Page<ProductEntry> products = productRepository.findByIsAvailableTrue(pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Get product by ID
     */
    public Optional<ProductDTO> getProductById(String id) {
        Optional<ProductEntry> product = productRepository.findById(id);
        return product.map(this::convertToDTO);
    }

    /**
     * Search products by query
     */
    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        Page<ProductEntry> products = productRepository.searchProducts(query, pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Get products by category
     */
    public Page<ProductDTO> getProductsByCategory(String category, Pageable pageable) {
        Page<ProductEntry> products = productRepository.findByCategoryAndIsAvailableTrue(category, pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Get products by location
     */
    public Page<ProductDTO> getProductsByLocation(String location, Pageable pageable) {
        Page<ProductEntry> products = productRepository.findByLocationContainingIgnoreCaseAndIsAvailableTrue(location,
                pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Get products by tags
     */
    public Page<ProductDTO> getProductsByTags(List<String> tags, Pageable pageable) {
        Page<ProductEntry> products = productRepository.findByTagsInAndIsAvailableTrue(tags, pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Get products by seller
     */
    public Page<ProductDTO> getProductsBySeller(String sellerId, Pageable pageable) {
        Page<ProductEntry> products = productRepository.findBySellerIdAndIsAvailableTrue(sellerId, pageable);
        return products.map(this::convertToDTO);
    }

    /**
     * Create a new product
     */
    @Transactional
    public ProductDTO createProduct(String username, ProductRequest request) {
        // Get user information
        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        ProductEntry product = new ProductEntry();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImages(request.getImages());
        product.setSellerId(user.getId().toString());
        product.setSellerUsername(user.getUserName());
        product.setStockQuantity(request.getStockQuantity());
        product.setIsAvailable(true);
        product.setLocation(request.getLocation());
        product.setTags(request.getTags());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setViews(0);
        product.setRating(0.0);
        product.setReviewCount(0);

        ProductEntry savedProduct = productRepository.save(product);
        log.info("Product created successfully: {} by user: {}", savedProduct.getId(), username);

        return convertToDTO(savedProduct);
    }

    /**
     * Update an existing product
     */
    @Transactional
    public ProductDTO updateProduct(String productId, String username, ProductRequest request) {
        ProductEntry product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Check if user is the seller
        UserEntry user = userRepository.findByuserName(username);
        if (user == null || !product.getSellerId().equals(user.getId().toString())) {
            throw new RuntimeException("Unauthorized to update this product");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImages(request.getImages());
        product.setStockQuantity(request.getStockQuantity());
        product.setLocation(request.getLocation());
        product.setTags(request.getTags());
        product.setUpdatedAt(LocalDateTime.now());

        ProductEntry savedProduct = productRepository.save(product);
        log.info("Product updated successfully: {} by user: {}", savedProduct.getId(), username);

        return convertToDTO(savedProduct);
    }

    /**
     * Delete a product (supports admin deletion)
     */
    @Transactional
    public void deleteProduct(String productId, String username) {
        ProductEntry product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        UserEntry user = userRepository.findByuserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Check if user is the seller OR an admin
        boolean isOwner = product.getSellerId().equals(user.getId().toString());
        boolean isAdmin = user.getRoles().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized to delete this product");
        }

        // Soft delete by marking as unavailable
        product.setIsAvailable(false);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
        log.info("Product deleted successfully: {} by user: {} (admin: {})", productId, username, isAdmin);
    }

    /**
     * Get total count of products
     */
    public long getProductCount() {
        return productRepository.count();
    }

    /**
     * Get count of available products
     */
    public long getAvailableProductCount() {
        return productRepository.countByIsAvailableTrue();
    }

    /**
     * Convert ProductEntry to ProductDTO
     */
    private ProductDTO convertToDTO(ProductEntry product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .images(product.getImages())
                .sellerId(product.getSellerId())
                .sellerUsername(product.getSellerUsername())
                .stockQuantity(product.getStockQuantity())
                .isAvailable(product.getIsAvailable())
                .location(product.getLocation())
                .tags(product.getTags())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .views(product.getViews())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .build();
    }
}