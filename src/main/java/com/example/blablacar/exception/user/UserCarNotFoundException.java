package com.example.blablacar.exception.user;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 */
public class UserCarNotFoundException extends RuntimeException {

    public UserCarNotFoundException(long id) {
        super("Car not found with id: " + id);
    }
}
