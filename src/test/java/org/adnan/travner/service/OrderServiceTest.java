package org.adnan.travner.service;

import org.adnan.travner.dto.CreateOrderRequest;
import org.adnan.travner.dto.OrderDTO;
import org.adnan.travner.entry.OrderEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ForbiddenException;
import org.adnan.travner.exception.InvalidOrderStateException;
import org.adnan.travner.repository.OrderRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Version;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Object-level authorization regression tests for order operations. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private final ObjectId sellerId = new ObjectId();
    private final ObjectId otherUserId = new ObjectId();
    private final ObjectId orderId = new ObjectId();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private OrderEntry paidOrder;

    @BeforeEach
    void setUp() {
        paidOrder = OrderEntry.builder()
                .id(orderId)
                .orderNumber("ORD-TEST")
                .status(OrderEntry.OrderStatus.PAID)
                .items(List.of(OrderEntry.OrderItem.builder()
                        .productId("p1")
                        .sellerId(sellerId.toHexString())
                        .build()))
                .build();
    }

    @Test
    void sellerCanFulfillOwnOrder() {
        when(userRepository.findByuserName("seller")).thenReturn(user("seller", sellerId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        OrderDTO result = orderService.fulfillOrder("seller", orderId.toHexString());

        assertEquals(OrderEntry.OrderStatus.FULFILLED, result.getStatus());
        verify(orderRepository).save(paidOrder);
    }

    @Test
    void adminCanFulfillAnyOrder() {
        when(userRepository.findByuserName("admin")).thenReturn(user("admin", otherUserId, List.of("ADMIN")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        OrderDTO result = orderService.fulfillOrder("admin", orderId.toHexString());

        assertEquals(OrderEntry.OrderStatus.FULFILLED, result.getStatus());
    }

    @Test
    void unrelatedUserCannotFulfillOrder() {
        when(userRepository.findByuserName("intruder")).thenReturn(user("intruder", otherUserId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(ForbiddenException.class,
                () -> orderService.fulfillOrder("intruder", orderId.toHexString()));
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void nonAdminCannotCancelOrderThroughService() {
        when(userRepository.findByuserName("intruder")).thenReturn(user("intruder", otherUserId, List.of("USER")));

        assertThrows(ForbiddenException.class,
                () -> orderService.adminCancelOrder("intruder", orderId.toHexString()));
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void orderCarriesOptimisticVersion() throws Exception {
        Field version = OrderEntry.class.getDeclaredField("version");
        assertNotNull(version.getAnnotation(Version.class));
    }

    @Test
    void sellerCanConfirmCashFromPending_onlySellerOrAdminCanReachPaid() {
        paidOrder.setStatus(OrderEntry.OrderStatus.PENDING);
        paidOrder.setVersion(3L);
        paidOrder.setPaymentInfo(OrderEntry.PaymentInfo.builder()
                .paymentMethod("CASH_ON_DELIVERY")
                .paymentStatus(OrderEntry.PaymentStatus.PENDING)
                .build());
        when(userRepository.findByuserName("seller")).thenReturn(user("seller", sellerId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));
        when(orderRepository.save(any(OrderEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.confirmCashOrder("seller", orderId.toHexString());

        assertEquals(OrderEntry.OrderStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        assertEquals(OrderEntry.PaymentStatus.COMPLETED, result.getPaymentInfo().getPaymentStatus());
        assertNotNull(result.getPaymentInfo().getPaidAt());
        assertEquals(3L, result.getVersion());
        verify(orderRepository).save(paidOrder);
    }

    @Test
    void buyerCannotConfirmTheirOwnOrderAsPaid() {
        paidOrder.setStatus(OrderEntry.OrderStatus.PENDING);
        when(userRepository.findByuserName("buyer")).thenReturn(user("buyer", otherUserId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(ForbiddenException.class,
                () -> orderService.confirmCashOrder("buyer", orderId.toHexString()));
        assertEquals(OrderEntry.OrderStatus.PENDING, paidOrder.getStatus());
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void adminCanConfirmCash() {
        paidOrder.setStatus(OrderEntry.OrderStatus.PENDING);
        when(userRepository.findByuserName("admin")).thenReturn(user("admin", otherUserId, List.of("ADMIN")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));
        when(orderRepository.save(any(OrderEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(OrderEntry.OrderStatus.PAID,
                orderService.confirmCashOrder("admin", orderId.toHexString()).getStatus());
    }

    @Test
    void cashConfirmationRejectsTerminalAndAlreadyPaidOrders() {
        when(userRepository.findByuserName("seller")).thenReturn(user("seller", sellerId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.confirmCashOrder("seller", orderId.toHexString()));
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void fulfillRejectsOrderThatWasNeverPaid() {
        paidOrder.setStatus(OrderEntry.OrderStatus.PENDING);
        when(userRepository.findByuserName("seller")).thenReturn(user("seller", sellerId, List.of("USER")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.fulfillOrder("seller", orderId.toHexString()));
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void buyerCancelSetsAuditFieldsAndRejectsPaidOrders() {
        paidOrder.setStatus(OrderEntry.OrderStatus.PENDING);
        paidOrder.setUserId("buyer");
        when(userRepository.findByuserName("buyer")).thenReturn(user("buyer", otherUserId, List.of("USER")));
        when(orderRepository.findByIdAndUserId(orderId, "buyer")).thenReturn(Optional.of(paidOrder));
        when(orderRepository.save(any(OrderEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO cancelled = orderService.cancelOrder("buyer", orderId.toHexString());

        assertEquals(OrderEntry.OrderStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals("buyer", cancelled.getCancelledBy());

        paidOrder.setStatus(OrderEntry.OrderStatus.PAID);
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.cancelOrder("buyer", orderId.toHexString()));
    }

    @Test
    void adminCannotCancelRefundedOrder() {
        paidOrder.setStatus(OrderEntry.OrderStatus.REFUNDED);
        when(userRepository.findByuserName("admin")).thenReturn(user("admin", otherUserId, List.of("ADMIN")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.adminCancelOrder("admin", orderId.toHexString()));
        verify(orderRepository, never()).save(any(OrderEntry.class));
    }

    @Test
    void createOrderRejectsNonCashPaymentMethods() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .paymentInfo(CreateOrderRequest.PaymentInfoRequest.builder()
                        .paymentMethod("CREDIT_CARD")
                        .build())
                .build();
        when(userRepository.findByuserName("buyer")).thenReturn(user("buyer", otherUserId, List.of("USER")));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder("buyer", request));
        verifyNoInteractions(cartService);
    }

    private UserEntry user(String username, ObjectId id, List<String> roles) {
        UserEntry user = new UserEntry();
        user.setUserName(username);
        user.setId(id);
        user.setRoles(roles);
        return user;
    }
}
