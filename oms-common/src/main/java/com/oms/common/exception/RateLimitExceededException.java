package com.oms.common.exception;

/**
 * Thrown when rate limit is exceeded.
 */
public class RateLimitExceededException extends ApiException {
    
    public RateLimitExceededException() {
        super(
            "Rate limit exceeded. Please try again later.",
            "RATE_LIMIT_EXCEEDED",
            429
        );
    }
    
    public RateLimitExceededException(long retryAfterSeconds) {
        super(
            String.format("Rate limit exceeded. Retry after %d seconds.", retryAfterSeconds),
            "RATE_LIMIT_EXCEEDED",
            429
        );
    }
}
