package com.example.blablacar.exception.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Thrown when a review is submitted or edited outside its open review window.
 */
public class ReviewWindowClosedException extends RuntimeException {
    public ReviewWindowClosedException(final String message) {
        super(message);
    }
}