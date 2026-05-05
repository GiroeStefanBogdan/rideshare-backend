package com.example.blablacar.exception;

import com.example.blablacar.dto.auth.ErrorResponseDto;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.exception.ride.RideDateTooDistantException;
import com.example.blablacar.exception.ride.RideNotFoundException;
import com.example.blablacar.exception.user.InvalidAgeException;
import com.example.blablacar.exception.user.UserCarNotFoundException;
import com.example.blablacar.exception.user.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRideNotFoundShouldReturn404() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleRideNotFound(new RideNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void handleForbiddenRideShouldReturn404() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleForbiddenRide(new ForbiddenRideException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleRideDateTooDistantShouldReturn400() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleRideDateTooDistant(new RideDateTooDistantException());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleInvalidRideStopShouldReturn422() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleInvalidRideStop(new InvalidRideStopException());

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleUserNotFoundShouldReturn404() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleUserNotFound(new UserNotFoundException("test"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleUserCarNotFoundShouldReturn404() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleUserCarNotFound(new UserCarNotFoundException(1L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleInvalidAgeShouldReturn400() {
        ResponseEntity<ErrorResponseDto> response =
                handler.notChildPermission(new InvalidAgeException("too young"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
