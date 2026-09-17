package com.example.blablacar.exception.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Thrown when a user acts on a review they do not own.
 */
public class ReviewForbiddenException extends RuntimeException {
    public ReviewForbiddenException(final String message) {
        super(message);
    }
}