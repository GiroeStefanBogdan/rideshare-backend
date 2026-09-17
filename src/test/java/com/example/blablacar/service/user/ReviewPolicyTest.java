package com.example.blablacar.service.user;

import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserReview;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewPolicyTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T10:00Z");
    private static final OffsetDateTime DROPOFF = NOW.minusDays(2);

    private static UserReview review(final ReviewStatus status) {
        UserReview review = new UserReview(new User(), new User(), 4, "fine", LocalDate.now(),
                ReviewAuthorRole.PASSENGER);
        review.setStatus(status);
        review.setWindowEndsAt(ReviewWindowPolicy.windowEndsAt(DROPOFF));
        review.setLatestSharedDropoffAt(DROPOFF);
        return review;
    }

    private static UserReview submitted() {
        UserReview review = review(ReviewStatus.PENDING);
        review.setSubmittedAt(NOW.minusHours(1));
        return review;
    }

    private static UserReview published() {
        UserReview review = review(ReviewStatus.PUBLISHED);
        review.setSubmittedAt(NOW.minusDays(2));
        review.setPublishedAt(NOW.minusDays(2));
        review.setPublishedScore(4);
        review.setPublishedDetails("fine");
        review.setPublishedRole(ReviewAuthorRole.PASSENGER);
        return review;
    }

    @Test
    void openPendingReviewCanBeSubmitted() {
        assertTrue(ReviewPolicy.canSubmit(review(ReviewStatus.PENDING), NOW));
    }

    @Test
    void publicationLocksTheCycleEvenWhileTimeRemains() {
        UserReview locked = published();
        assertTrue(ReviewPolicy.isWindowOpen(locked, NOW));
        assertFalse(ReviewPolicy.canSubmit(locked, NOW));
    }

    @Test
    void hiddenAndExpiredReviewsCannotBeSubmitted() {
        UserReview hidden = review(ReviewStatus.PENDING);
        hidden.setHiddenAt(NOW.minusMinutes(5));
        assertFalse(ReviewPolicy.canSubmit(hidden, NOW));

        UserReview expired = review(ReviewStatus.PENDING);
        expired.setWindowEndsAt(NOW.minusSeconds(1));
        assertFalse(ReviewPolicy.canSubmit(expired, NOW));
    }

    @Test
    void reciprocalSubmissionPublishesBoth() {
        assertTrue(ReviewPolicy.shouldPublish(submitted(), submitted(), NOW));
    }

    @Test
    void reciprocalSubmissionsFromDifferentCyclesStayPrivate() {
        UserReview older = submitted();
        UserReview newer = submitted();
        newer.setLatestSharedDropoffAt(DROPOFF.plusDays(1));

        assertFalse(ReviewPolicy.shouldPublish(older, newer, NOW));
        assertFalse(ReviewPolicy.shouldPublish(newer, older, NOW));
    }

    @Test
    void aLoneReviewWaitsForItsDeadlineUnlessTheCounterpartSubmits() {
        UserReview mine = submitted();
        assertFalse(ReviewPolicy.shouldPublish(mine, null, NOW));

        UserReview counterpart = review(ReviewStatus.PENDING);
        assertFalse(ReviewPolicy.shouldPublish(mine, counterpart, NOW));

        mine.setWindowEndsAt(NOW.minusSeconds(1));
        assertTrue(ReviewPolicy.shouldPublish(mine, null, NOW));
    }

    @Test
    void unsubmittedContentIsNeverPublished() {
        assertFalse(ReviewPolicy.shouldPublish(review(ReviewStatus.PENDING), published(), NOW));
    }

    @Test
    void publishedContentStaysVisibleWhileAnAmendmentIsPending() {
        UserReview amended = published();
        amended.setStatus(ReviewStatus.PENDING);
        amended.setSubmittedAt(NOW.minusMinutes(1));
        amended.setScore(2);

        assertTrue(ReviewPolicy.isVisibleToPublic(amended));
        assertTrue(ReviewPolicy.shouldPublish(amended, submitted(), NOW));
    }

    @Test
    void neverPublishedAndHiddenReviewsAreInvisible() {
        assertFalse(ReviewPolicy.isVisibleToPublic(submitted()));

        UserReview hidden = published();
        hidden.setHiddenAt(NOW.minusMinutes(1));
        hidden.setStatus(ReviewStatus.HIDDEN);
        assertFalse(ReviewPolicy.isVisibleToPublic(hidden));
    }

    @Test
    void onlyAStrictlyNewerDropoffReopensTheWindow() {
        UserReview existing = published();
        ReflectionTestUtils.setField(existing, "latestSharedDropoffAt", NOW.minusDays(10));

        assertFalse(ReviewPolicy.opensNewCycle(existing, NOW.minusDays(10), NOW));
        assertFalse(ReviewPolicy.opensNewCycle(existing, NOW.minusDays(20), NOW));
        assertTrue(ReviewPolicy.opensNewCycle(existing, NOW.minusDays(1), NOW));
    }

    @Test
    void aNewerDropoffOutsideItsOwnWindowDoesNotReopen() {
        UserReview existing = published();
        existing.setLatestSharedDropoffAt(NOW.minusDays(30));

        assertFalse(ReviewPolicy.opensNewCycle(existing, NOW.minusDays(20), NOW));
        assertFalse(ReviewPolicy.opensNewCycle(existing, null, NOW));
    }
}