package com.oms.common.event;

import com.oms.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event for order state changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    
    private UUID eventId;
    private UUID orderId;
    private String eventType;  // CREATED, CONFIRMED, FAILED, SHIPPED, CANCELLED, STATUS_UPDATED
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String customerId;
    private List<OrderItemEvent> items;
    private String reason;
    private Instant timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private String sku;
        private int quantity;
    }
    
    public static OrderEvent created(UUID orderId, String customerId, List<OrderItemEvent> items) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .eventType("CREATED")
                .previousStatus(null)
                .newStatus(OrderStatus.PENDING)
                .customerId(customerId)
                .items(items)
                .timestamp(Instant.now())
                .build();
    }
    
    public static OrderEvent statusChanged(UUID orderId, OrderStatus from, OrderStatus to, String reason) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .eventType("STATUS_UPDATED")
                .previousStatus(from)
                .newStatus(to)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
    }
}
