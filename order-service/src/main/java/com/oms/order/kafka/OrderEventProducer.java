package com.oms.order.kafka;

import com.oms.common.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for order events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    public void publishOrderEvent(OrderEvent event) {
        log.info("Publishing order event: type={}, orderId={}", 
                event.getEventType(), event.getOrderId());
        
        kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order event for order: {}", 
                                event.getOrderId(), ex);
                    } else {
                        log.debug("Order event published successfully: {}", event.getOrderId());
                    }
                });
    }
}
