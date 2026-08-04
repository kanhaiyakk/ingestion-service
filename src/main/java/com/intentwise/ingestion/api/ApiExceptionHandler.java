package com.intentwise.ingestion.api;

import com.intentwise.ingestion.config.SourceConfigException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps domain/validation failures to small JSON error bodies instead of default Spring error pages. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SourceConfigException.class)
    public ResponseEntity<ApiErrorResponse> handleUnknownSource(SourceConfigException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidArgument(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value for '" + e.getName() + "': " + e.getValue();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }
}
