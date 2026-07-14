package com.example.blablacar.exception;

import com.example.blablacar.dto.auth.ErrorResponseDto;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.exception.ride.InvalidRidePricingException;
import com.example.blablacar.exception.ride.InvalidRideScheduleException;
import com.example.blablacar.exception.ride.RideDateTooDistantException;
import com.example.blablacar.exception.ride.RideNotFoundException;
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
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailConflict(final EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("email", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(final MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        message
                ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(final UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "User Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(UserCarNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserCarNotFound(final UserCarNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "Car Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidAgeException.class)
    public ResponseEntity<ErrorResponseDto> notChildPermission(final InvalidAgeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Age should be at least 18",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleRideNotFound(final RideNotFoundException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Ride Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(ForbiddenRideException.class)
    public ResponseEntity<ErrorResponseDto> handleForbiddenRide(final ForbiddenRideException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Ride Not Found",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(RideDateTooDistantException.class)
    public ResponseEntity<ErrorResponseDto> handleRideDateTooDistant(
            final RideDateTooDistantException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Ride date too distant",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidRideStopException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRideStop(final InvalidRideStopException exception) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Invalid ride stop",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidRideScheduleException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRideSchedule(
            final InvalidRideScheduleException exception) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Invalid ride schedule",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidRidePricingException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRidePricing(
            final InvalidRidePricingException exception) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "Invalid ride pricing",
                        exception.getMessage()
                ));
    }
}
