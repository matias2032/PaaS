package com.dev58.paasbackend.common.exception;

import com.dev58.paasbackend.auth.exception.InvalidCredentialsException;
import com.dev58.paasbackend.auth.exception.InvalidResetTokenException;
import com.dev58.paasbackend.auth.exception.UserAlreadyExistsException;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.common.dto.ErrorResponseDTO;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.OrganizationMemberAlreadyExistsException;
import com.dev58.paasbackend.organization.exception.OrganizationMemberNotFoundException;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.OrganizationSlugAlreadyExistsException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.billing.exception.PlanNotFoundException;
import com.dev58.paasbackend.billing.exception.PlanPriceNotFoundException;
import com.dev58.paasbackend.billing.exception.PlanSlugAlreadyExistsException;
import com.dev58.paasbackend.billing.exception.SubscriptionAlreadyExistsException;
import com.dev58.paasbackend.billing.exception.SubscriptionNotFoundException;
import com.dev58.paasbackend.project.exception.GitConnectionNotFoundException;
import com.dev58.paasbackend.project.exception.GitProviderNotFoundException;
import com.dev58.paasbackend.project.exception.ProjectNotFoundException;
import com.dev58.paasbackend.service.exception.DeploymentException;
import com.dev58.paasbackend.service.exception.DeploymentNotFoundException;
import com.dev58.paasbackend.service.exception.DomainNotFoundException;
import com.dev58.paasbackend.service.exception.EnvironmentVariableNotFoundException;
import com.dev58.paasbackend.service.exception.ServiceBuildConfigNotFoundException;
import com.dev58.paasbackend.service.exception.ServiceNotFoundException;
import com.dev58.paasbackend.service.exception.ServiceRepositoryNotFoundException;
import com.dev58.paasbackend.service.exception.ServiceResourceConfigNotFoundException;
import com.dev58.paasbackend.service.exception.ServiceTypeNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Central mapping from domain exceptions to HTTP responses. Without
 * this, every unhandled exception fell through to Spring's default
 * error page (or, before /error was made public in SecurityConfig,
 * appeared as a misleading 403 — see the CORS/security session
 * notes). Keeps every error response in ErrorResponseDTO shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidResetToken(
            InvalidResetTokenException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleOrganizationNotFound(
            OrganizationNotFoundException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(OrganizationMemberNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleOrganizationMemberNotFound(
            OrganizationMemberNotFoundException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handlePermissionDenied(
            PermissionDeniedException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler({OrganizationSlugAlreadyExistsException.class, OrganizationMemberAlreadyExistsException.class, OrganizationInactiveException.class, PlanSlugAlreadyExistsException.class, SubscriptionAlreadyExistsException.class})
    public ResponseEntity<ErrorResponseDTO> handleOrganizationConflict(
            RuntimeException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({PlanNotFoundException.class, PlanPriceNotFoundException.class, SubscriptionNotFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleBillingNotFound(
            RuntimeException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({ProjectNotFoundException.class, GitProviderNotFoundException.class, GitConnectionNotFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleProjectNotFound(
            RuntimeException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
            ServiceTypeNotFoundException.class,
            ServiceNotFoundException.class,
            ServiceRepositoryNotFoundException.class,
            ServiceBuildConfigNotFoundException.class,
            ServiceResourceConfigNotFoundException.class,
            EnvironmentVariableNotFoundException.class,
            DomainNotFoundException.class,
            DeploymentNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleServiceNotFound(
            RuntimeException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DeploymentException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeploymentException(
            DeploymentException ex, HttpServletRequest request
    ) {
        // Today the only thrower is createDeployment()'s trigger_type
        // validation — a client input error, hence 400. When this
        // module gains a real CoolifyClient and starts wrapping actual
        // deployment failures (build/API errors) with a cause, this
        // mapping will likely need to split: no cause -> 400 (bad
        // input), cause present -> 502 (upstream Coolify failure).
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(
            Exception ex, HttpServletRequest request
    ) {
        // Fallback for anything not explicitly mapped above. Keeps the
        // response shape consistent instead of leaking a stack trace
        // or Spring's default HTML error page.
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status, String message, HttpServletRequest request
    ) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}