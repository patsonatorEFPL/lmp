package com.lmp.shared.web;

import com.lmp.shared.dto.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Gestionnaire d'exceptions REST pour les endpoints /api/**.
 * Retourne toujours du JSON (ApiResponse) au lieu des templates Thymeleaf.
 * Priorité haute (@Order(1)) pour intercepter avant GlobalExceptionHandler.
 */
@RestControllerAdvice(basePackages = {
        "com.lmp.catalog.web.api",
        "com.lmp.billing.web.api",
        "com.lmp.portal.api",
        "com.lmp.shared.web.api"
})
@Order(1)
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex, HttpServletResponse response) {
        if (response.isCommitted()) {
            logger.debug("RuntimeException on committed response (e.g. SSE stream): {}", ex.toString());
            return null;
        }
        if (ex.getMessage() != null && ex.getMessage().contains("non trouvé")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
        logger.error("Unexpected API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne s'est produite"));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException ex, HttpServletResponse response) {
        if (response.isCommitted()) {
            logger.debug("NullPointerException on committed response (e.g. SSE): {}", ex.toString());
            return null;
        }
        logger.error("NullPointerException in API: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Erreur de configuration interne"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex, HttpServletResponse response) {
        if (response.isCommitted()) {
            logger.debug("Unhandled exception on committed response (e.g. SSE client closed): {}", ex.toString());
            return null;
        }
        logger.error("Unhandled API exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur inattendue s'est produite"));
    }
}
