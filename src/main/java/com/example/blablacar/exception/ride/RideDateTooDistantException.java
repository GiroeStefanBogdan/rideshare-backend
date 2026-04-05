package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The ride date is too far in the future")
public class RideDateTooDistantException extends RuntimeException {
    public RideDateTooDistantException() {
    }
}
