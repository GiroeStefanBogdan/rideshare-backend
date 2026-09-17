package com.example.blablacar.dto.review;

import com.example.blablacar.model.enums.ReviewAuthorRole;

import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * One counterpart the acting user may review, derived from a shared ride rather than from anything
 * the client asserts. {@code existingReviewId} is set when a review already exists, in which case
 * {@code canSubmit} tells whether its window still allows an amendment.
 */
public record ReviewEligibilityDto(
        long targetUserId,
        String targetName,
        ReviewAuthorRole role,
        long rideId,
        OffsetDateTime dropoffAt,
        OffsetDateTime windowEndsAt,
        Long existingReviewId,
        boolean canSubmit
) {
}