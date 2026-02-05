package com.oms.common.exception;

import com.oms.common.enums.OrderStatus;

/**
 * Thrown when an invalid order state transition is attempted.
 */
public class InvalidOrderStateException extends ApiException {
    
    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(
            String.format("Invalid state transition from '%s' to '%s'", 
                currentStatus, targetStatus),
            "INVALID_ORDER_STATE",
            400
        );
    }
    
    public InvalidOrderStateException(String message) {
        super(message, "INVALID_ORDER_STATE", 400);
    }
}
