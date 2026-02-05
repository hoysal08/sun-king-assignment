package com.oms.common.enums;

/**
 * Order status lifecycle enumeration.
 */
public enum OrderStatus {
    PENDING("Order created, awaiting inventory confirmation"),
    CONFIRMED("Inventory reserved, order confirmed"),
    PROCESSING("Order is being prepared"),
    SHIPPED("Order has been shipped"),
    DELIVERED("Order delivered to customer"),
    CANCELLED("Order was cancelled"),
    FAILED("Order failed due to system/inventory issues");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if transition to target status is valid.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED || target == FAILED;
            case CONFIRMED -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED, CANCELLED, FAILED -> false;
        };
    }
}
