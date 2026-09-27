package com.example.notification.common;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        return body(
                HttpStatus.BAD_REQUEST,
                e.getBindingResult().getFieldErrors().stream()
                        .map(x -> x.getField() + ": " + x.getDefaultMessage())
                        .toList()
                        .toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, Object>> cv(ConstraintViolationException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus s, String m) {
        return ResponseEntity.status(s)
                .body(
                        Map.of(
                                "timestamp",
                                Instant.now(),
                                "status",
                                s.value(),
                                "error",
                                s.getReasonPhrase(),
                                "message",
                                m));
    }
}
