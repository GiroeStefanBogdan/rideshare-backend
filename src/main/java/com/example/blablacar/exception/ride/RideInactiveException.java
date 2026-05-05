package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Ride is no longer active")
public class RideInactiveException extends RuntimeException {
}
