package org.adnan.travner.service;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private UserEntry user(String username, ObjectId id, List<String> roles) {
        UserEntry user = new UserEntry();
        user.setUserName(username);
        user.setId(id);
        user.setRoles(roles);
        return user;
    }
}
