package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Ride has already departed")
public class RideDepartedException extends RuntimeException {
}
