package com.example.blablacar.service.user;

import com.example.blablacar.model.enums.ReviewStatus;
import com.example.blablacar.model.user.UserReview;

import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * The review rules, kept free of persistence and HTTP so they can be reasoned about directly.
 * A review is one directional, lifetime piece of feedback: its window opens when the passenger's
 * scheduled drop-off passes and closes fourteen days later. Content becomes public only when both
 * counterparts have submitted for the same cycle, or when the later window closes. Publication
 * locks the cycle, so further edits wait for a later shared ride to reopen the window.
 */
public final class ReviewPolicy {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;
    public static final int MAX_COMMENT_LENGTH = 1000;
    public static final String DELETED_MEMBER = "Deleted member";

    private ReviewPolicy() {
    }

    /** Whether the review's current window is still open at {@code now}. */
    public static boolean isWindowOpen(final UserReview review, final OffsetDateTime now) {
        return review.getWindowEndsAt() != null && now.isBefore(review.getWindowEndsAt());
    }

    /**
     * Whether the author may submit or amend content in the current cycle. Hidden and published
     * reviews are locked: publication ends the cycle even while its window has time left.
     */
    public static boolean canSubmit(final UserReview review, final OffsetDateTime now) {
        return isWindowOpen(review, now)
                && review.getHiddenAt() == null
                && review.getStatus() != ReviewStatus.PUBLISHED;
    }

    /** Whether the review holds current-cycle content that has not been published yet. */
    public static boolean isPendingSubmission(final UserReview review) {
        return review != null
                && review.getSubmittedAt() != null
                && review.getStatus() == ReviewStatus.PENDING
                && review.getHiddenAt() == null;
    }

    /**
     * Whether this review's current content should become public now: reciprocally once its
     * counterpart also has a pending submission, or unilaterally once its own window closes.
     */
    public static boolean shouldPublish(final UserReview review, final UserReview counterpart,
                                        final OffsetDateTime now) {
        if (!isPendingSubmission(review)) {
            return false;
        }

        boolean sameCycle = isPendingSubmission(counterpart)
                && review.getLatestSharedDropoffAt() != null
                && counterpart.getLatestSharedDropoffAt() != null
                && review.getLatestSharedDropoffAt().isEqual(counterpart.getLatestSharedDropoffAt());
        return sameCycle || !isWindowOpen(review, now);
    }

    /**
     * Whether the review is publicly visible. Public sight follows published content, not cycle
     * state, so a pending amendment never hides what was already made public.
     */
    public static boolean isVisibleToPublic(final UserReview review) {
        return review.getPublishedScore() != null && review.getHiddenAt() == null;
    }

    /** The score contributing to reputation, or null when the review contributes nothing. */
    public static Integer visibleScore(final UserReview review) {
        return isVisibleToPublic(review) ? review.getPublishedScore() : null;
    }

    /**
     * Whether a later shared ride reopens this review's window. Only a strictly newer drop-off
     * counts, so replaying the same finished ride cannot extend the window indefinitely.
     */
    public static boolean opensNewCycle(final UserReview review, final OffsetDateTime newerDropoff,
                                        final OffsetDateTime now) {
        if (newerDropoff == null) {
            return false;
        }

        OffsetDateTime latest = review.getLatestSharedDropoffAt();
        return (latest == null || newerDropoff.isAfter(latest))
                && ReviewWindowPolicy.canReviewAfter(newerDropoff, now);
    }
}