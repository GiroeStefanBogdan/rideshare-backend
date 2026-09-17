package com.example.blablacar.exception.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 */
public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(final String message) {
        super(message);
    }
}