package com.omnicharge.common.exception;

import com.omnicharge.common.dto.ErrorResponse;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired(required = false)
    private LogEventPublisher logEventPublisher;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.NOT_FOUND, "ResourceNotFoundException");
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.BAD_REQUEST, "BadRequestException");
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.UNAUTHORIZED, "UnauthorizedException");
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.FORBIDDEN, "ForbiddenException");
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.CONFLICT, "DuplicateResourceException");
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        // Publish detailed log event
        publishExceptionLog(ex, request, HttpStatus.SERVICE_UNAVAILABLE, "ServiceUnavailableException");
        
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed on {}: {}", request.getRequestURI(), errors);
        
        // Publish detailed validation error log
        if (logEventPublisher != null) {
            String message = String.format(
                "[VALIDATION-ERROR] URI: %s | Errors: %s",
                request.getRequestURI(),
                errors
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .level("WARN")
                    .eventType("VALIDATION")
                    .logger(GlobalExceptionHandler.class.getName())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        // Publish detailed log event with full stack trace
        publishExceptionLog(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "UnhandledException");
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Helper method to publish exception log events with full context and stack trace.
     */
    private void publishExceptionLog(Exception ex, HttpServletRequest request, 
                                     HttpStatus status, String exceptionType) {
        if (logEventPublisher == null) {
            return; // LogEventPublisher not available (e.g., in tests)
        }
        
        try {
            // Extract full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // Build detailed message with context
            String message = String.format(
                "[EXCEPTION] Type: %s | Status: %d | URI: %s | Message: %s",
                exceptionType,
                status.value(),
                request.getRequestURI(),
                ex.getMessage()
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .level(status.is5xxServerError() ? "ERROR" : "WARN")
                    .eventType("EXCEPTION")
                    .logger(GlobalExceptionHandler.class.getName())
                    .message(message)
                    .stackTrace(stackTrace)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .build();
            
            logEventPublisher.publish(logEvent);
        } catch (Exception e) {
            // Don't let logging failures break exception handling
            log.error("Failed to publish exception log event", e);
        }
    }
}
