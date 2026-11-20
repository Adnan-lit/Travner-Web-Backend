package org.adnan.travner.market;

import org.adnan.travner.TravnerApplication;
import org.adnan.travner.config.BaseIntegrationTest;
import org.adnan.travner.entry.OrderEntry;
import org.adnan.travner.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Real MongoDB check for the P2-05 optimistic lock. Skipped locally without
 * Docker; CI runs it against the Testcontainers replica set.
 */
@SpringBootTest(classes = TravnerApplication.class)
@ActiveProfiles("test")
class OrderLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void concurrentStatusTransitionsAreRejectedByVersionCheck() {
        OrderEntry saved = orderRepository.save(OrderEntry.builder()
                .orderNumber("P2-05-1")
                .userId("buyer")
                .status(OrderEntry.OrderStatus.PENDING)
                .build());
        assertNotNull(saved.getVersion());

        OrderEntry first = orderRepository.findById(saved.getId()).orElseThrow();
        OrderEntry second = orderRepository.findById(saved.getId()).orElseThrow();

        first.setStatus(OrderEntry.OrderStatus.PAID);
        OrderEntry updated = orderRepository.save(first);
        assertEquals(first.getVersion(), updated.getVersion());

        second.setStatus(OrderEntry.OrderStatus.CANCELLED);
        assertThrows(OptimisticLockingFailureException.class, () -> orderRepository.save(second));

        assertEquals(OrderEntry.OrderStatus.PAID,
                orderRepository.findById(saved.getId()).orElseThrow().getStatus());
    }
}
