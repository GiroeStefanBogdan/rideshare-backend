package com.example.blablacar.dto.review;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * A user's reviews together with their combined reputation summary.
 */
public record UserReviewsDto(
        RatingSummaryDto summary,
        List<ReviewResponseDto> reviews
) {
}