package com.digitalheroes.Page.Pulse.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUrlFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrlFormat(InvalidUrlFormatException ex, HttpServletRequest http) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_URL", ex.getMessage(), http.getRequestURI());
    }

    @ExceptionHandler({InvalidContentTypeException.class, UnsupportedMimeTypeException.class})
    public ResponseEntity<ErrorResponse> handleInvalidContentType(InvalidContentTypeException ex, HttpServletRequest http) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "NOT_AN_HTML_PAGE", ex.getMessage(), http.getRequestURI());
    }

    @ExceptionHandler(SocketTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleConnectionTimeOut(SocketTimeoutException ex, HttpServletRequest http) {
        return buildErrorResponse(HttpStatus.REQUEST_TIMEOUT, "TIMEOUT", "Targeted website took too long to respond", http.getRequestURI());
    }

    @ExceptionHandler(HttpStatusException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamStatus(HttpStatusException ex, HttpServletRequest http) {
        int status = ex.getStatusCode();
        String errorCode = "REMOTE_ERROR";
        if (status == 404) errorCode = "NOT_FOUND";
        else if (status >= 500) errorCode = "UPSTREAM_ERROR";
        String message = String.format("Upstream returned HTTP %d: %s", status, ex.getMessage());
        return buildErrorResponse(HttpStatus.valueOf(status), errorCode, message, http.getRequestURI());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIoFailures(IOException ex, HttpServletRequest http) {
        // IOException but not a SocketTimeoutException
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "NETWORK_ERROR", "Network error when contacting target URL", http.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralFailures(Exception ex, HttpServletRequest http) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Internal Server Error", http.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String error, String message, String path) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}
