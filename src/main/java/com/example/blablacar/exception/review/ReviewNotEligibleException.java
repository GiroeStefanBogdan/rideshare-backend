package com.example.blablacar.exception.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Thrown when a review is attempted for a counterpart with no qualifying shared experience, so the
 * author never gained review eligibility.
 */
public class ReviewNotEligibleException extends RuntimeException {
    public ReviewNotEligibleException(final String message) {
        super(message);
    }
}