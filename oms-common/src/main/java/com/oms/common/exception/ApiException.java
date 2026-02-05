package com.oms.common.exception;

import lombok.Getter;

/**
 * Base exception for all API exceptions.
 */
@Getter
public abstract class ApiException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    private final Object details;
    
    protected ApiException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }
    
    protected ApiException(String message, String errorCode, int httpStatus, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }
    
    protected ApiException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }
}
