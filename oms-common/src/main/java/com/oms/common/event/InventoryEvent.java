package com.oms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for inventory changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    
    private UUID eventId;
    private String eventType;  // RESERVED, RELEASED, DEDUCTED, UPDATED
    private String sku;
    private int quantity;
    private UUID orderId;      // Optional, for order-related events
    private int previousStock;
    private int newStock;
    private Instant timestamp;
    
    public static InventoryEvent reserved(String sku, int quantity, UUID orderId, int previousStock) {
        return InventoryEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("RESERVED")
                .sku(sku)
                .quantity(quantity)
                .orderId(orderId)
                .previousStock(previousStock)
                .newStock(previousStock - quantity)
                .timestamp(Instant.now())
                .build();
    }
    
    public static InventoryEvent released(String sku, int quantity, UUID orderId, int previousStock) {
        return InventoryEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("RELEASED")
                .sku(sku)
                .quantity(quantity)
                .orderId(orderId)
                .previousStock(previousStock)
                .newStock(previousStock + quantity)
                .timestamp(Instant.now())
                .build();
    }
}
