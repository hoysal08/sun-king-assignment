package com.oms.common.exception;

import java.util.Map;

/**
 * Thrown when there is insufficient inventory to fulfill an order.
 */
public class InsufficientInventoryException extends ApiException {
    
    public InsufficientInventoryException(String sku, int requested, int available) {
        super(
            String.format("Insufficient inventory for SKU '%s': requested %d, available %d", 
                sku, requested, available),
            "INSUFFICIENT_INVENTORY",
            400,
            Map.of(
                "sku", sku,
                "requested", requested,
                "available", available
            )
        );
    }
}
