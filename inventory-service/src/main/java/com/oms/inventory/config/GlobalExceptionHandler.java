package com.oms.inventory.config;

import com.oms.common.dto.ApiResponse;
import com.oms.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for consistent error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom API exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        log.error("API Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getDetails(), traceId));
    }

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        
        log.warn("Validation error: {}", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", "Invalid request data", errors, traceId));
    }

    /**
     * Handle missing request header errors.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        String headerName = ex.getHeaderName();
        Map<String, String> details = new HashMap<>();
        details.put("missingHeader", headerName);
        
        log.warn("Missing required request header: {}", headerName);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("MISSING_HEADER", 
                        "Required request header '" + headerName + "' is missing", details, traceId));
    }

    /**
     * Handle data integrity violations (e.g., duplicate SKU).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        log.error("Data integrity violation: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DATA_CONFLICT", "Data integrity violation: " + ex.getMostSpecificCause().getMessage(), null, traceId));
    }

    /**
     * Handle optimistic locking failures.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        log.error("Optimistic locking failure - concurrent modification detected");
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONCURRENT_MODIFICATION", 
                        "The resource was modified by another request. Please retry.", null, traceId));
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId(request);
        log.error("Unexpected error: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred", null, traceId));
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return traceId != null ? traceId : UUID.randomUUID().toString().substring(0, 8);
    }
}
