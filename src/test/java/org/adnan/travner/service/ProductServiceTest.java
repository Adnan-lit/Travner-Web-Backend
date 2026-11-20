package org.adnan.travner.service;

import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.dto.ProductRequest;
import org.adnan.travner.entry.MediaEntry;
import org.adnan.travner.entry.ProductEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.repository.MediaRepository;
import org.adnan.travner.repository.ProductRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Product-media ownership checks introduced in P1-04. */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private final ObjectId productId = new ObjectId();
    private final ObjectId sellerId = new ObjectId();
    private final ObjectId otherId = new ObjectId();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private ProductService productService;

    private ProductEntry product;

    @BeforeEach
    void setUp() {
        product = new ProductEntry();
        product.setId(productId);
        product.setSellerId(sellerId.toHexString());
        product.setSellerUsername("seller");
    }

    @Test
    void sellerCanManageProductMedia() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByuserName("seller")).thenReturn(user("seller", sellerId, List.of("USER")));

        assertDoesNotThrow(() -> productService.assertCanManageProductMedia(productId.toHexString(), "seller"));
    }

    @Test
    void adminCanManageAnyProductMedia() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByuserName("admin")).thenReturn(user("admin", otherId, List.of("ADMIN")));

        assertDoesNotThrow(() -> productService.assertCanManageProductMedia(productId.toHexString(), "admin"));
    }

    @Test
    void unrelatedUserCannotManageProductMedia() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByuserName("intruder")).thenReturn(user("intruder", otherId, List.of("USER")));

        assertThrows(ForbiddenException.class,
                () -> productService.assertCanManageProductMedia(productId.toHexString(), "intruder"));
    }

    @Test
    void missingProductIsNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.assertCanManageProductMedia(productId.toHexString(), "seller"));
    }

    @Test
    void createProduct_mediaUrlIsNotDuplicatedWhenAlsoSuppliedAsImage() {
        ObjectId mediaId = new ObjectId();
        String mediaUrl = "/api/media/" + mediaId;
        MediaEntry media = MediaEntry.builder().id(mediaId).uploadedBy(otherId.toHexString()).build();
        ProductRequest request = ProductRequest.builder()
                .name("Product")
                .category("category")
                .price(BigDecimal.ONE)
                .stockQuantity(1)
                .images(List.of(mediaUrl))
                .mediaIds(List.of(mediaId.toHexString()))
                .build();

        when(userRepository.findByuserName("admin")).thenReturn(user("admin", otherId, List.of("ADMIN")));
        when(productRepository.save(any(ProductEntry.class))).thenAnswer(invocation -> {
            ProductEntry saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(new ObjectId());
            }
            return saved;
        });
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        ProductDTO result = productService.createProduct("admin", request);

        assertEquals(1, result.getImages().stream().filter(mediaUrl::equals).count());
    }

    private UserEntry user(String username, ObjectId id, List<String> roles) {
        UserEntry user = new UserEntry();
        user.setUserName(username);
        user.setId(id);
        user.setRoles(roles);
        return user;
    }
}
