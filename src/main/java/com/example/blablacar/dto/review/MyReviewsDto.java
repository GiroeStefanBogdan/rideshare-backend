package com.example.blablacar.dto.review;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * The acting user's own review workspace: their reputation, the reviews they received, the reviews
 * they wrote, and the counterparts awaiting their feedback.
 */
public record MyReviewsDto(
        RatingSummaryDto summary,
        List<ReviewResponseDto> received,
        List<ReviewResponseDto> authored,
        List<ReviewEligibilityDto> toWrite
) {
}