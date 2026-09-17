package com.example.blablacar.exception.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Thrown when a published review is edited before a later shared ride reopens its window.
 * Publication locks the current cycle even while calendar time remains.
 */
public class ReviewEditLockedException extends RuntimeException {
    public ReviewEditLockedException(final String message) {
        super(message);
    }
}