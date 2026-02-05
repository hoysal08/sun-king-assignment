package com.oms.common.exception;

import java.util.UUID;

/**
 * Thrown when an order is not found.
 */
public class OrderNotFoundException extends ApiException {
    
    public OrderNotFoundException(UUID orderId) {
        super(
            String.format("Order not found with ID: %s", orderId),
            "ORDER_NOT_FOUND",
            404
        );
    }
    
    public OrderNotFoundException(String message) {
        super(message, "ORDER_NOT_FOUND", 404);
    }
}
