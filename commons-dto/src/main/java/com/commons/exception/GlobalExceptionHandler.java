package com.commons.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.FieldError;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import com.commons.dto.ErrorCodes;
import com.commons.dto.ErrorResponse;

/**
 * Centralized error mapping to your commons-dto envelope.
 * Status codes align with your OpenAPI conventions.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Agent / AI Guardrails ----------

    @ExceptionHandler(GuardrailViolationException.class)
    public ResponseEntity<ErrorResponse> handleGuardrailViolation(GuardrailViolationException ex) {
        log.warn("guardrail_violation_mapped check={} risk={} message={}",
                ex.checkName(), ex.riskLevel(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(
                        ErrorCodes.FORBIDDEN,
                        "This request was blocked by the agent safety guardrails.",
                        "check=" + ex.checkName() + " risk=" + ex.riskLevel() + " reason=" + ex.getMessage()
                ));
    }

    // ---------- Auth / Access ----------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.debug("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(err(ErrorCodes.INVALID_JWT, "The provided JWT token is invalid or expired", ex.getMessage()));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException ex) {
        log.debug("JwtAuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(err(ErrorCodes.INVALID_CREDS, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.debug("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.INSUFFICIENT_SCOPE, "The current token does not have the required permission.", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScope(InsufficientScopeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.INSUFFICIENT_SCOPE, "The current token does not have the required permission.", ex.getMessage()));
    }

    @ExceptionHandler(ToolAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleToolAccessDenied(ToolAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.INSUFFICIENT_SCOPE, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler({OwnershipViolationException.class, ConfirmationOwnershipException.class, OwnerAccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleOwnershipViolation(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.OWNERSHIP_VIOLATION, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.FORBIDDEN, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(err(ErrorCodes.UNAUTHORIZED, ex.getMessage(), ex.getMessage()));
    }

    // ---------- Bad Request / Tool Input ----------

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.INVALID_INPUT, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(MissingCustomerContextException.class)
    public ResponseEntity<ErrorResponse> handleMissingCustomerContext(MissingCustomerContextException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.MISSING_CUSTOMER_CONTEXT, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidToolInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToolInput(InvalidToolInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.INVALID_INPUT, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(McpToolException.class)
    public ResponseEntity<ErrorResponse> handleMcpToolException(McpToolException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.INVALID_INPUT, ex.getMessage(), ex.getMessage()));
    }

    // ---------- Confirmation ----------

    @ExceptionHandler(InvalidConfirmationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidConfirmation(InvalidConfirmationTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.CONFIRMATION_TOKEN_INVALID, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(ConfirmationTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleConfirmationExpired(ConfirmationTokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.CONFIRMATION_TOKEN_EXPIRED, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(ConfirmationToolMismatchException.class)
    public ResponseEntity<ErrorResponse> handleConfirmationToolMismatch(ConfirmationToolMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.CONFIRMATION_TOOL_MISMATCH, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePreconditionRequired(PreconditionRequiredException ex) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(err(ErrorCodes.PRECONDITION_REQUIRED, ex.getMessage(), ex.getMessage()));
    }

    // ---------- Downstream / Feign ----------

    @ExceptionHandler(DownstreamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamNotFound(DownstreamNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.DOWNSTREAM_NOT_FOUND, ex.getSafeMessage(), ex.getMessage()));
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamUnavailable(DownstreamUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(err(ErrorCodes.DOWNSTREAM_UNAVAILABLE, ex.getSafeMessage(), ex.getMessage()));
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamService(DownstreamServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(err(ErrorCodes.DOWNSTREAM_ERROR, ex.getSafeMessage(), ex.getMessage()));
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponse> handleFeignForbidden(FeignException.Forbidden ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err(ErrorCodes.INSUFFICIENT_SCOPE,
                        "The current token does not have the required permission to perform this tool action.",
                        "Downstream banking service rejected the request with 403 Forbidden."));
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<ErrorResponse> handleFeignUnauthorized(FeignException.Unauthorized ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(err(ErrorCodes.DOWNSTREAM_UNAUTHORIZED,
                        "Authentication is required or the token is invalid.",
                        "Downstream banking service rejected the request with 401 Unauthorized."));
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleFeignNotFound(FeignException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.DOWNSTREAM_NOT_FOUND,
                        "The requested downstream banking resource was not found.",
                        "Downstream banking service rejected the request with 404 Not Found."));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignGeneric(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        if (status.is5xxServerError()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(err(ErrorCodes.DOWNSTREAM_ERROR,
                            "A downstream banking service failed while executing the request.",
                            "Downstream banking service returned a server error."));
        }

        return ResponseEntity.status(status)
                .body(err(ErrorCodes.DOWNSTREAM_ERROR,
                        "A downstream banking service rejected the request.",
                        "Downstream banking service returned HTTP " + ex.status() + "."));
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(err(ErrorCodes.DOWNSTREAM_ERROR, ex.getMessage(), ex.getMessage()));
    }

    // ---------- Domain Not Found ----------

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.ACCOUNT_NOT_FOUND, "The account ID does not exist", ex.getMessage()));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.CUSTOMER_NOT_FOUND, "The customer ID does not exist", ex.getMessage()));
    }

    @ExceptionHandler(ConsentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConsentNotFound(ConsentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.CONSENT_MISSING, "Consent Missing", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(err(ErrorCodes.NOT_FOUND, "Resource not found", ex.getMessage()));
    }

    // ---------- Business / State ----------

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(err(ErrorCodes.CONFLICT, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(err(ErrorCodes.INSUFFICIENT_FUNDS, "Insufficient funds", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.INVALID_TRANSITION, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleVersionMismatch(VersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(err(ErrorCodes.VERSION_MISMATCH, "Stale version or ETag", ex.getMessage()));
    }

    // ---------- Validation ----------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.VALIDATION_ERROR, "Constraint violation", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String detail = (ex.getMostSpecificCause() == null)
                ? ex.getMessage()
                : ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.VALIDATION_ERROR, "Malformed JSON request", detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleFieldValidation(MethodArgumentNotValidException ex) {
        String firstMessage = "Input validation failed";
        String firstField = null;
        StringBuilder all = new StringBuilder();

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        for (FieldError fe : fieldErrors) {
            if (firstField == null) {
                firstField = fe.getField();
                if (fe.getDefaultMessage() != null) {
                    firstMessage = fe.getDefaultMessage();
                }
            }
            if (all.length() > 0) all.append("; ");
            all.append(fe.getField()).append(": ").append(fe.getDefaultMessage());
        }

        String details = (firstField != null)
                ? "field=" + firstField + " | " + all
                : all.toString();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.VALIDATION_ERROR, firstMessage, details));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.VALIDATION_ERROR, ex.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err(ErrorCodes.INVALID_INPUT, ex.getMessage(), ex.getMessage()));
    }

    // ---------- Fallback ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(err(ErrorCodes.INTERNAL_ERROR, "Something went wrong", ex.getMessage()));
    }

    // ---------- helper ----------

    private ErrorResponse err(String code, String message, String details) {
        return ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .build();
    }
}
