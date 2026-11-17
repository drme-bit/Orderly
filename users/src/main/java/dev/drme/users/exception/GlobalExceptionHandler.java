package dev.drme.users.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------------
    // 4xx
    // ---------------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        return build(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    /**
     * {@code @Valid @RequestBody} failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = validationMessage(ex.getBindingResult().getFieldErrors());

        log.warn("Request validation failed: {}", message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Validation failures on method parameters (path variables, query params).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed: {}", message);

        return build(
                HttpStatus.BAD_REQUEST,
                message.isBlank() ? "Validation failed" : message,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("Constraint violation: {}", message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Type mismatch, e.g. {@code /users/not-a-uuid}.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid value for '" + ex.getName() + "'";

        log.warn("{}: {}", message, ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        // Details go to the log only: Jackson messages expose class names and payload snippets.
        log.warn("Malformed request body: {}", ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Method {} is not supported for {}", ex.getMethod(), request.getRequestURI());

        HttpHeaders headers = new HttpHeaders();
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        if (supported != null) {
            headers.setAllow(supported);
        }

        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method " + ex.getMethod() + " is not supported",
                request,
                headers
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Unsupported media type: {}", ex.getContentType());

        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request
    ) {
        log.warn("Not acceptable: {}", ex.getMessage());

        return build(HttpStatus.NOT_ACCEPTABLE, "Requested media type is not supported", request);
    }

    /**
     * Exceptions raised by the application layer with an explicit status.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        log.warn("{}: {}", ex.getStatusCode(), ex.getReason());

        return build(
                ex.getStatusCode(),
                ex.getReason() != null ? ex.getReason() : reasonOf(ex.getStatusCode()),
                request
        );
    }

    /**
     * Illegal state transitions in the domain (e.g. updating a non-active user).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        log.warn("Domain state violation: {}", ex.getMessage());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Concurrent update detected by {@code @Version} on the JPA entity.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());

        return build(HttpStatus.CONFLICT, "The user was modified by another request, please retry", request);
    }

    /**
     * Unique email race and any other constraint violation. The SQL details stay in the log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation", ex);

        return build(HttpStatus.CONFLICT, "Request conflicts with the current state of the data", request);
    }

    // ---------------------------------------------------------------------
    // 5xx
    // ---------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception", ex);

        // Never leak internal messages to the client.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), request);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String validationMessage(List<FieldError> errors) {
        String message = errors.stream()
                .map(error -> error.getField() + ": " +
                        (error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));

        return message.isBlank() ? "Validation failed" : message;
    }

    private static ResponseEntity<ApiError> build(
            HttpStatusCode status,
            String message,
            HttpServletRequest request
    ) {
        return build(status, message, request, new HttpHeaders());
    }

    private static ResponseEntity<ApiError> build(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            HttpHeaders headers
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                reasonOf(status),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private static String reasonOf(HttpStatusCode status) {
        return status instanceof HttpStatus httpStatus
                ? httpStatus.getReasonPhrase()
                : "Error";
    }
}
