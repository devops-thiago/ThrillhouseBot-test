package com.thrillhouse.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderProcessorTest {

    @Test
    void processBatch_handlesZeroAmountOrderGracefully() {
        PaymentGateway mockGateway = mock(PaymentGateway.class);
        // Stub every charge as successful, including zero-amount ones, so the
        // test doesn't need a live gateway to check the batch-level plumbing.
        when(mockGateway.charge(anyString(), any())).thenReturn(new ChargeResult(true, "conf-1"));

        OrderProcessor processor = new OrderProcessor(mockGateway);
        Order zeroAmountOrder = new Order("o-1", "cust-1", BigDecimal.ZERO, OrderStatus.PENDING, Instant.now());

        BatchResult result = processor.processBatch(List.of(zeroAmountOrder));

        assertNotNull(result);
        verify(mockGateway).charge("cust-1", BigDecimal.ZERO);
    }

    @Test
    void findDuplicateOrderIds_flagsRepeatedId() {
        OrderService service = new OrderService();
        Instant now = Instant.now();
        List<Order> orders = List.of(
                new Order("o-1", "cust-1", BigDecimal.TEN, OrderStatus.PENDING, now),
                new Order("o-1", "cust-1", BigDecimal.TEN, OrderStatus.PENDING, now),
                new Order("o-2", "cust-2", BigDecimal.ONE, OrderStatus.PENDING, now));

        List<String> duplicates = service.findDuplicateOrderIds(orders);

        assertEquals(2, duplicates.size());
        assertEquals("o-1", duplicates.get(0));
    }
}
