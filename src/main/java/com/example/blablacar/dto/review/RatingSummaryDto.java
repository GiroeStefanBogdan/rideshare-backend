package com.example.blablacar.dto.review;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * The combined reputation of a user. {@code average} is null, never zero, when no review has been
 * published: an unrated member is not the same as a badly rated one.
 */
public record RatingSummaryDto(
        Double average,
        int count
) {
    public static RatingSummaryDto unrated() {
        return new RatingSummaryDto(null, 0);
    }
}