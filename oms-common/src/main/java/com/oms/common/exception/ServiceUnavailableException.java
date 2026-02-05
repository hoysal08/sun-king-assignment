package com.oms.common.exception;

/**
 * Thrown when a required external service is unavailable.
 */
public class ServiceUnavailableException extends ApiException {
    
    public ServiceUnavailableException(String serviceName) {
        super(
            String.format("Service '%s' is currently unavailable. Please try again later.", serviceName),
            "SERVICE_UNAVAILABLE",
            503
        );
    }
    
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(
            String.format("Service '%s' is currently unavailable. Please try again later.", serviceName),
            "SERVICE_UNAVAILABLE",
            503,
            cause
        );
    }
}
