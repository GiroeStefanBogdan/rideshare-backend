package com.example.blablacar.dto.review;

import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;

import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * A review as shown in lists. {@code score}, {@code details} and {@code role} always describe the
 * published content, so a pending amendment never changes what the public already sees. The author
 * additionally receives {@code pendingScore}, {@code pendingDetails}, {@code canEdit} and
 * {@code canSubmit} to act on their own review.
 */
public record ReviewResponseDto(
        long id,
        Long authorId,
        String authorName,
        Long targetUserId,
        Integer score,
        String details,
        ReviewAuthorRole role,
        OffsetDateTime publishedAt,
        boolean edited,
        boolean pending,
        boolean published,
        Integer pendingScore,
        String pendingDetails,
        OffsetDateTime windowEndsAt,
        boolean canSubmit,
        ReviewStatus status
) {
}