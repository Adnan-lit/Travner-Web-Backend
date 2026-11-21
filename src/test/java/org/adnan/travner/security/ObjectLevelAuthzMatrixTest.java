package org.adnan.travner.security;

import org.adnan.travner.config.CorsConfig;
import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.JwtConfig;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.config.SpringSecurity;
import org.adnan.travner.controller.ChatController;
import org.adnan.travner.controller.MarketController;
import org.adnan.travner.controller.MediaController;
import org.adnan.travner.controller.OrderController;
import org.adnan.travner.entry.ProductEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.InvalidOrderStateException;
import org.adnan.travner.repository.ProductRepository;
import org.adnan.travner.service.ChatAttachmentService;
import org.adnan.travner.service.ChatService;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.service.OrderService;
import org.adnan.travner.service.ProductService;
import org.adnan.travner.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.adnan.travner.security.JwtTestTokenFactory.ADMIN;
import static org.adnan.travner.security.JwtTestTokenFactory.ADMIN_ID;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_A;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_A_ID;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_B;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_B_ID;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Object-level matrix: a service-level ownership denial must surface as a
 * generic 403 (not 404/500) on every REST path, and malformed ObjectIds as 400.
 * This also locks in the principal passed to the service for user A vs user B.
 */
@WebMvcTest(controllers = {
        OrderController.class, MediaController.class, ChatController.class, MarketController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SpringSecurity.class, JwtConfig.class, CorsConfig.class})
class ObjectLevelAuthzMatrixTest {

    private static final String BAD_ID = "not-an-object-id";
    private static final String INTERNAL_REASON = "secret ownership detail";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    private RateLimiter rateLimiter;
    @MockBean
    private ClientAddressResolver clientAddressResolver;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private OrderService orderService;
    @MockBean
    private MediaService mediaService;
    @MockBean
    private ChatService chatService;
    @MockBean
    private ChatAttachmentService chatAttachmentService;
    @MockBean
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @MockBean
    private ProductService productService;
    @MockBean
    private ProductRepository productRepository;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        when(rateLimiter.isApiAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isChatAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isUploadAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isLoginAllowed(anyString())).thenReturn(true);
        when(rateLimiter.allowApi(anyString())).thenReturn(RateLimitDecision.permit());
        when(rateLimiter.allowChat(anyString())).thenReturn(RateLimitDecision.permit());
        when(rateLimiter.allowUpload(anyString())).thenReturn(RateLimitDecision.permit());
        when(rateLimiter.allowLogin(anyString())).thenReturn(RateLimitDecision.permit());
        when(rateLimiter.allowRefresh(anyString())).thenReturn(RateLimitDecision.permit());
        when(clientAddressResolver.resolveClientIp(any())).thenReturn("127.0.0.1");
        userToken = "Bearer " + JwtTestTokenFactory.token(jwtEncoder, USER_A_ID, USER_A, "USER");
        adminToken = "Bearer " + JwtTestTokenFactory.token(jwtEncoder, ADMIN_ID, ADMIN, "ADMIN");
    }

    @Test
    void orderFulfill_deniedByOwnershipCheck_returnsGeneric403() throws Exception {
        when(orderService.fulfillOrder(eq(USER_A), anyString()))
                .thenThrow(new ForbiddenException(INTERNAL_REASON));

        assertGenericForbidden(post("/api/market/orders/507f191e810c19729de860ea/fulfill"));
    }

    @Test
    void payEndpoint_isRemoved_buyerCannotSelfMarkPaid() throws Exception {
        mockMvc.perform(post("/api/market/orders/507f191e810c19729de860ea/pay")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmCash_deniedByOwnershipCheck_returnsGeneric403() throws Exception {
        when(orderService.confirmCashOrder(eq(USER_A), anyString()))
                .thenThrow(new ForbiddenException(INTERNAL_REASON));

        mockMvc.perform(post("/api/market/orders/507f191e810c19729de860ea/confirm-cash")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(content().string(not(containsString(INTERNAL_REASON))));
    }

    @Test
    void invalidOrderTransition_returns409WithoutLeakingInternalState() throws Exception {
        when(orderService.confirmCashOrder(eq(USER_A), anyString()))
                .thenThrow(new InvalidOrderStateException("internal status detail"));

        mockMvc.perform(post("/api/market/orders/507f191e810c19729de860ea/confirm-cash")
                        .header("Authorization", userToken))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("internal status detail"))));
    }

    @Test
    void concurrentOrderModification_returns409() throws Exception {
        when(orderService.confirmCashOrder(eq(USER_A), anyString()))
                .thenThrow(new OptimisticLockingFailureException("concurrent write"));

        mockMvc.perform(post("/api/market/orders/507f191e810c19729de860ea/confirm-cash")
                        .header("Authorization", userToken))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("concurrent write"))));
    }

    @Test
    void orderRead_deniedByOwnershipCheck_returnsGeneric403() throws Exception {
        when(orderService.getOrderById(eq(USER_A), anyString()))
                .thenThrow(new ForbiddenException(INTERNAL_REASON));

        assertGenericForbidden(get("/api/market/orders/507f191e810c19729de860ea"));
    }

    @Test
    void adminCancel_deniedByService_returnsGeneric403() throws Exception {
        when(orderService.adminCancelOrder(eq(ADMIN), anyString()))
                .thenThrow(new ForbiddenException(INTERNAL_REASON));

        mockMvc.perform(post("/api/market/orders/507f191e810c19729de860ea/admin-cancel")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(content().string(not(containsString(INTERNAL_REASON))));
    }

    @Test
    void malformedOrderId_returns400() throws Exception {
        when(orderService.getOrderById(eq(USER_A), eq(BAD_ID)))
                .thenThrow(new IllegalArgumentException("invalid ObjectId format"));

        mockMvc.perform(get("/api/market/orders/" + BAD_ID)
                        .header("Authorization", userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mediaDelete_deniedByOwnershipCheck_returnsGeneric403() throws Exception {
        org.mockito.Mockito.doThrow(new ForbiddenException(INTERNAL_REASON))
                .when(mediaService).deleteMedia(anyString(), eq(USER_A));

        assertGenericForbidden(delete("/api/media/507f191e810c19729de860ec"));
    }

    @Test
    void malformedMediaId_returns400() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("invalid ObjectId format"))
                .when(mediaService).deleteMedia(eq(BAD_ID), eq(USER_A));

        mockMvc.perform(delete("/api/media/" + BAD_ID)
                        .header("Authorization", userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatMessages_deniedByParticipantCheck_returnsGeneric403() throws Exception {
        when(chatService.getConversationMessages(anyString(), eq(USER_A_ID), any()))
                .thenThrow(new ForbiddenException(INTERNAL_REASON));

        assertGenericForbidden(get("/api/chat/conversations/conv1/messages"));
    }

    @Test
    void chatMarkRead_deniedByParticipantCheck_returnsGeneric403() throws Exception {
        org.mockito.Mockito.doThrow(new ForbiddenException(INTERNAL_REASON))
                .when(chatService).markMessagesAsRead(anyString(), eq(USER_A_ID));

        assertGenericForbidden(post("/api/chat/conversations/conv1/mark-read"));
    }

    @Test
    void productMediaUpload_deniedBySellerCheck_returnsGeneric403() throws Exception {
        org.mockito.Mockito.doThrow(new ForbiddenException(INTERNAL_REASON))
                .when(productService).assertCanManageProductMedia(anyString(), eq(USER_A));

        mockMvc.perform(multipart("/api/market/products/507f191e810c19729de860ea/upload-media")
                        .file(new MockMultipartFile("file", "p.txt", "text/plain", new byte[]{1}))
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(content().string(not(containsString(INTERNAL_REASON))));
    }

    @Test
    void productMediaDelete_delegatesFullCleanupToService() throws Exception {
        when(productRepository.findById(any())).thenReturn(Optional.of(new ProductEntry()));
        String productId = "507f191e810c19729de860ea";
        String mediaUrl = "/api/media/507f191e810c19729de860eb";

        mockMvc.perform(delete("/api/market/products/" + productId + "/media")
                        .param("mediaUrl", mediaUrl)
                        .header("Authorization", userToken))
                .andExpect(status().isOk());

        verify(mediaService).deleteProductMedia(productId, mediaUrl);
    }

    @Test
    void principalUsername_isPassedThroughForEachUser() throws Exception {
        mockMvc.perform(get("/api/market/orders").header("Authorization", userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/market/orders").header("Authorization", "Bearer "
                        + JwtTestTokenFactory.token(jwtEncoder, USER_B_ID, USER_B, "USER")))
                .andExpect(status().isOk());

        verify(orderService).getUserOrders(USER_A);
        verify(orderService).getUserOrders(USER_B);
    }

    @Test
    void chatEndpoint_passesJwtSubjectIdNotDisplayUsername() throws Exception {
        mockMvc.perform(get("/api/chat/conversations").header("Authorization", userToken))
                .andExpect(status().isOk());
        org.mockito.Mockito.doNothing().when(chatService).markMessagesAsRead(anyString(), anyString());
        mockMvc.perform(post("/api/chat/conversations/conv1/mark-read").header("Authorization", userToken))
                .andExpect(status().isOk());

        verify(chatService).getUserConversations(eq(USER_A_ID), any());
        verify(chatService).markMessagesAsRead("conv1", USER_A_ID);
        verify(chatService, org.mockito.Mockito.never()).getUserConversations(eq(USER_A), any());
    }

    private void assertGenericForbidden(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        mockMvc.perform(builder.header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(content().string(not(containsString(INTERNAL_REASON))));
    }
}
