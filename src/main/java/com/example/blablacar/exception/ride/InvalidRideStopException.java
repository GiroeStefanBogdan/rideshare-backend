package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The specified ride stop id doesn't exist")
public class InvalidRideStopException extends RuntimeException {

    public InvalidRideStopException() {
        super("The specified Ride Stop is invalid");
    }

    public InvalidRideStopException(final String message) {
        super(message);
    }
}
