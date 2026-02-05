package com.oms.common.exception;

/**
 * Thrown when a product is not found in inventory.
 */
public class ProductNotFoundException extends ApiException {
    
    public ProductNotFoundException(String sku) {
        super(
            String.format("Product not found with SKU: %s", sku),
            "PRODUCT_NOT_FOUND",
            404
        );
    }
}
