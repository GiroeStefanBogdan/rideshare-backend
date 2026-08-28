package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class InvalidRideScheduleException extends RuntimeException {
    public InvalidRideScheduleException(final String message) {
        super(message);
    }
}
