package com.example.blablacar.exception;

import com.example.blablacar.dto.ErrorResponseDto;
import com.example.blablacar.exception.user.EmailAlreadyExistsException;
import com.example.blablacar.exception.user.InvalidAgeException;
import com.example.blablacar.exception.user.UserCarNotFoundException;
import com.example.blablacar.exception.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailConflict(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("email", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleRegisterValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "User Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(UserCarNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserCarNotFound(UserCarNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "Car Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidAgeException.class)
    public ResponseEntity<ErrorResponseDto> notChildPermission(InvalidAgeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Age should be at least 18",
                        exception.getMessage()
                ));
    }
}
