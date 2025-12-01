package com.xstream.clouddesktop.controller.advice;

import com.xstream.clouddesktop.client.guacamole.exception.GuacamoleException;
import com.xstream.clouddesktop.client.proxmox.exception.ProxmoxException;
import com.xstream.clouddesktop.dto.response.ErrorResponse;
import com.xstream.clouddesktop.service.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Custom Service Exceptions ---

    @ExceptionHandler(DesktopNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDesktopNotFound(DesktopNotFoundException ex, HttpServletRequest request) {
        log.warn("DesktopNotFoundException: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.of("DESKTOP_NOT_FOUND", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DesktopAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDesktopAlreadyExists(DesktopAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("DesktopAlreadyExistsException: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.of("DESKTOP_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({DesktopNotReadyException.class, InvalidDesktopStateException.class})
    public ResponseEntity<ErrorResponse> handleInvalidState(RuntimeException ex, HttpServletRequest request) {
        log.warn("Invalid state exception: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.of("INVALID_STATE", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DesktopProvisioningException.class)
    public ResponseEntity<ErrorResponse> handleProvisioning(DesktopProvisioningException ex, HttpServletRequest request) {
        log.error("DesktopProvisioningException: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.of("PROVISIONING_ERROR", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- External Service Exceptions ---

    @ExceptionHandler(ProxmoxException.class)
    public ResponseEntity<ErrorResponse> handleProxmox(ProxmoxException ex, HttpServletRequest request) {
        log.error("ProxmoxException: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.of("VIRTUALIZATION_SERVICE_ERROR", "Virtual machine service unavailable.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(GuacamoleException.class)
    public ResponseEntity<ErrorResponse> handleGuacamole(GuacamoleException ex, HttpServletRequest request) {
        log.error("GuacamoleException: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.of("GATEWAY_SERVICE_ERROR", "Remote desktop gateway unavailable.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    // --- Spring Validation and Binding Exceptions ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        log.warn("Validation error: {} for request {}", fieldErrors, request.getRequestURI());
        ErrorResponse error = ErrorResponse.validationError("Request validation failed", fieldErrors, request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String field = cv.getPropertyPath().toString();
            String message = cv.getMessage();
            errors.put(field.substring(field.lastIndexOf('.') + 1), message);
        });

        log.warn("Constraint violation: {} for request {}", errors, request.getRequestURI());
        ErrorResponse error = ErrorResponse.validationError("Request validation failed", errors, request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.of("INVALID_REQUEST_BODY", "Invalid or malformed request body.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // --- Generic Catch-All Exception ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.of("INTERNAL_SERVER_ERROR", "An unexpected error occurred.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
