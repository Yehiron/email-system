package com.learning.email;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()).distinct().toList();
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message",
            "Revisa los campos: " + String.join(", ", fields)));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> malformed() {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_JSON", "message", "El cuerpo JSON no es válido."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> upstream(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("code", "REQUEST_FAILED", "message", ex.getReason()));
    }
}
