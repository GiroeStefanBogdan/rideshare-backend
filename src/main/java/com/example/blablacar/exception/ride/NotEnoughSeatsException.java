package com.example.blablacar.exception.ride;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Not enough available seats for the requested segment")
public class NotEnoughSeatsException extends RuntimeException {
}
