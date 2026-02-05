package com.oms.inventory.config;

import com.oms.common.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Rate limiter using Bucket4j for API traffic control.
 * Prevents system overload during peak traffic.
 */
@Component
@Slf4j
public class RateLimiter {

    @Value("${app.rate-limit.requests-per-minute:200}")
    private int requestsPerMinute;

    private Bucket bucket;

    @PostConstruct
    public void init() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
        log.info("Rate limiter initialized: {} requests per minute", requestsPerMinute);
    }

    /**
     * Check if request is within rate limit.
     * @throws RateLimitExceededException if limit exceeded
     */
    public void checkLimit() {
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded");
            throw new RateLimitExceededException();
        }
    }

    /**
     * Get available tokens for monitoring.
     */
    public long getAvailableTokens() {
        return bucket.getAvailableTokens();
    }
}
