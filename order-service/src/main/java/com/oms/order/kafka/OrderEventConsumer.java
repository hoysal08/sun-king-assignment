package com.oms.order.kafka;

import com.oms.common.event.OrderEvent;
import com.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing order events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event, Acknowledgment acknowledgment) {
        log.info("Received order event: type={}, orderId={}", 
                event.getEventType(), event.getOrderId());

        try {
            switch (event.getEventType()) {
                case "CREATED" -> {
                    // Process newly created order
                    orderService.processOrder(event.getOrderId());
                }
                case "STATUS_UPDATED", "CONFIRMED", "FAILED", "CANCELLED" -> {
                    // Log status changes - could trigger notifications here
                    log.info("Order {} status changed: {} -> {}", 
                            event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());
                }
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            acknowledgment.acknowledge();
            log.debug("Event acknowledged for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order event for {}: {}", 
                    event.getOrderId(), e.getMessage());
            // Let Kafka retry based on configuration
            throw e;
        }
    }
}
