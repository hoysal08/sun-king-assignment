package com.oms.inventory.kafka;

import com.oms.common.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for inventory events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
    
    @Value("${app.kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    public void publishInventoryEvent(InventoryEvent event) {
        log.info("Publishing inventory event: type={}, sku={}", event.getEventType(), event.getSku());
        kafkaTemplate.send(inventoryEventsTopic, event.getSku(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish inventory event for SKU: {}", event.getSku(), ex);
                    } else {
                        log.debug("Inventory event published successfully for SKU: {}", event.getSku());
                    }
                });
    }
}
