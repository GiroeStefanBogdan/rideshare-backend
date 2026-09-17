package com.example.blablacar.service.user;

import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 */
public final class ReviewWindowPolicy {

    private static final int WINDOW_DAYS = 14;

    private ReviewWindowPolicy() {
    }

    /**
     * The instant a shared ride's review window closes: exactly fourteen full days after the
     * passenger's scheduled drop-off. Null when no drop-off time is known (legacy rides).
     */
    public static OffsetDateTime windowEndsAt(final OffsetDateTime dropoff) {
        if (dropoff == null) {
            return null;
        }
        return dropoff.plusDays(WINDOW_DAYS);
    }

    /**
     * Whether a review may still be started at {@code now} for an experience whose scheduled
     * drop-off is {@code dropoff}. The window opens when the ride becomes past at the drop-off
     * instant and closes exactly fourteen days later. Rides without a drop-off never qualify.
     */
    public static boolean canReviewAfter(final OffsetDateTime dropoff, final OffsetDateTime now) {
        return dropoff != null && !now.isBefore(dropoff) && now.isBefore(windowEndsAt(dropoff));
    }
}
