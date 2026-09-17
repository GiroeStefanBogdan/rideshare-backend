package com.example.blablacar.service.user;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewWindowPolicyTest {
    private static final OffsetDateTime DROPOFF = OffsetDateTime.parse("2026-09-01T12:00Z");

    @Test
    void windowIsClosedBeforeTheDropoff() {
        assertFalse(ReviewWindowPolicy.canReviewAfter(DROPOFF, DROPOFF.minusSeconds(1)));
    }

    @Test
    void windowOpensAtTheDropoffInstant() {
        assertTrue(ReviewWindowPolicy.canReviewAfter(DROPOFF, DROPOFF));
    }

    @Test
    void windowIsClosedAtTheDeadlineAndOpenOneSecondBefore() {
        OffsetDateTime deadline = ReviewWindowPolicy.windowEndsAt(DROPOFF);
        assertFalse(ReviewWindowPolicy.canReviewAfter(DROPOFF, deadline));
        assertTrue(ReviewWindowPolicy.canReviewAfter(DROPOFF, deadline.minusSeconds(1)));
    }

    @Test
    void deadlineIsExactlyFourteenDaysAfterTheDropoff() {
        assertEquals(DROPOFF.plusDays(14), ReviewWindowPolicy.windowEndsAt(DROPOFF));
    }

    @Test
    void missingDropoffNeverOpensAWindow() {
        assertFalse(ReviewWindowPolicy.canReviewAfter(null, DROPOFF.plusDays(1)));
        assertNull(ReviewWindowPolicy.windowEndsAt(null));
    }
}
