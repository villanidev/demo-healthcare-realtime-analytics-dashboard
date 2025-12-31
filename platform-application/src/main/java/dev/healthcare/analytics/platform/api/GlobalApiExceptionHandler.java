package dev.healthcare.analytics.platform.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
        ApiError body = new ApiError(ex.getMessage() != null ? ex.getMessage() : "Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    public record ApiError(String message) {
    }
}
