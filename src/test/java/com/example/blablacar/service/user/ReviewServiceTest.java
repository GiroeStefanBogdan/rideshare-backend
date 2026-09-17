package com.example.blablacar.service.user;

import com.example.blablacar.dto.review.ReviewRequestDto;
import com.example.blablacar.dto.review.ReviewResponseDto;
import com.example.blablacar.exception.review.ReviewEditLockedException;
import com.example.blablacar.exception.review.ReviewNotEligibleException;
import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.model.user.UserReview;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.user.ReviewRepository;
import com.example.blablacar.repository.user.UserInfoRepository;
import com.example.blablacar.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T10:00Z");

    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserInfoRepository userInfos = mock(UserInfoRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);

    private final User actor = new User();
    private final User target = new User();
    private final UserInfo targetInfo = new UserInfo();

    private final ReviewService service = new ReviewService(reviews, users, userInfos, bookings,
            Clock.fixed(NOW.toInstant(), NOW.getOffset()));

    @BeforeEach
    void setUp() {
        actor.setId(1L);
        actor.setName("Ana");
        target.setId(2L);
        target.setName("Dan");
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(userInfos.findById(anyLong())).thenReturn(Optional.of(targetInfo));
        when(reviews.save(any(UserReview.class))).thenAnswer(call -> {
            UserReview saved = call.getArgument(0);
            if (saved.getId() == null) {
                // Stand in for the database assigning the identity on insert.
                ReflectionTestUtils.setField(saved, "id", 99L);
            }
            return saved;
        });
    }

    private Booking rideWithTargetAsDriver(final OffsetDateTime dropoff) {
        RideStop stop = new RideStop(null, null, (byte) 1, dropoff, (byte) 2, (short) 30);
        Ride ride = new Ride(target, null, null, List.of(stop), (byte) 3, (short) 30, dropoff, null);
        ReflectionTestUtils.setField(ride, "id", 7L);
        return new Booking(actor, ride, stop, stop, (byte) 1, 30);
    }

    private void actorTravelledWith(final OffsetDateTime dropoff) {
        when(bookings.findReviewableBookingsAsPassenger(anyLong(), any(), any()))
                .thenReturn(List.of(rideWithTargetAsDriver(dropoff)));
        when(bookings.findReviewableBookingsAsDriver(anyLong(), any(), any())).thenReturn(List.of());
    }

    private UserReview existing(final ReviewStatus status) {
        UserReview review = new UserReview(target, actor, 5, "old", LocalDate.now(),
                ReviewAuthorRole.PASSENGER);
        review.setStatus(status);
        review.setWindowEndsAt(NOW.plusDays(5));
        review.setSubmittedAt(NOW.minusDays(1));
        return review;
    }

    @Test
    void aLoneSubmissionStaysPrivateUntilTheCounterpartWrites() {
        actorTravelledWith(NOW.minusDays(1));
        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.empty());

        ReviewResponseDto response = service.submit(actor, new ReviewRequestDto(2L, 4, "pleasant"));

        assertTrue(response.pending());
        assertFalse(response.published());
        assertNull(response.score());
        assertEquals(4, response.pendingScore());
        assertEquals(ReviewStatus.PENDING, response.status());
    }

    @Test
    void mutualSubmissionsPublishBothReviews() {
        actorTravelledWith(NOW.minusDays(1));
        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.empty());
        UserReview counterpart = new UserReview(target, actor, 5, "great", LocalDate.now(),
                ReviewAuthorRole.DRIVER);
        counterpart.setStatus(ReviewStatus.PENDING);
        counterpart.setSubmittedAt(NOW.minusMinutes(30));
        counterpart.setLatestSharedDropoffAt(NOW.minusDays(1));
        counterpart.setWindowEndsAt(NOW.plusDays(13));
        when(reviews.findByReviewerIdAndTargetUserId(2L, 1L)).thenReturn(Optional.of(counterpart));
        when(reviews.findAllReceivedByTargetUserId(2L)).thenReturn(List.of());

        ReviewResponseDto response = service.submit(actor, new ReviewRequestDto(2L, 4, "pleasant"));

        assertTrue(response.published());
        assertEquals(4, response.score());
        assertEquals(ReviewStatus.PUBLISHED, response.status());
        assertEquals(ReviewStatus.PUBLISHED, counterpart.getStatus());
        assertEquals(5, counterpart.getPublishedScore());
        // Both sides published, so both reputations were recomputed.
        verify(userInfos, atLeastOnce()).save(targetInfo);
    }

    @Test
    void aLoneSubmissionPublishesOnceItsWindowHasClosed() {
        actorTravelledWith(NOW.minusDays(20));
        UserReview mine = existing(ReviewStatus.PENDING);
        mine.setWindowEndsAt(NOW.minusDays(6));
        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.of(mine));
        when(reviews.findByReviewerIdAndTargetUserId(2L, 1L)).thenReturn(Optional.empty());
        when(reviews.findAllReceivedByTargetUserId(1L)).thenReturn(List.of(mine));
        when(reviews.findAllReceivedByTargetUserId(2L)).thenReturn(List.of());

        service.getMyReviews(actor);

        assertEquals(ReviewStatus.PUBLISHED, mine.getStatus());
        assertEquals(mine.getScore(), mine.getPublishedScore());
    }

    @Test
    void publicationLocksFurtherEditsUntilALaterRide() {
        actorTravelledWith(NOW.minusDays(1));
        UserReview mine = existing(ReviewStatus.PUBLISHED);
        // The published cycle came from this same ride, so nothing strictly newer has happened.
        mine.setLatestSharedDropoffAt(NOW.minusDays(1));

        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.of(mine));

        assertThrows(ReviewEditLockedException.class,
                () -> service.submit(actor, new ReviewRequestDto(2L, 4, "changed my mind")));
    }

    @Test
    void aLaterSharedRideReopensAPublishedReviewWithoutAPriorRead() {
        actorTravelledWith(NOW.minusDays(2));
        UserReview mine = existing(ReviewStatus.PUBLISHED);
        mine.setPublishedScore(3);
        mine.setPublishedDetails("first impression");
        mine.setPublishedAt(NOW.minusDays(8));
        mine.setWindowEndsAt(NOW.minusDays(6));
        mine.setLatestSharedRideId(5L);
        mine.setLatestSharedDropoffAt(NOW.minusDays(20));

        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.of(mine));
        when(reviews.findByReviewerIdAndTargetUserId(2L, 1L)).thenReturn(Optional.empty());
        when(reviews.findAllReceivedByTargetUserId(2L)).thenReturn(List.of());

        ReviewResponseDto response = service.submit(actor, new ReviewRequestDto(2L, 4, "better this time"));

        assertEquals(ReviewStatus.PENDING, response.status());
        assertEquals(4, response.pendingScore());
        assertEquals(3, response.score());
        assertEquals(NOW.minusDays(2), mine.getLatestSharedDropoffAt());
        assertEquals(NOW.plusDays(12), mine.getWindowEndsAt());
        verify(reviews).save(mine);
    }

    @Test
    void authorReadPublishesAnOverdueSubmission() {
        UserReview mine = overdueAuthoredReview();
        when(reviews.findAllByReviewerIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(mine));

        assertTrue(service.getMyReviews(actor).authored().getFirst().published());
        assertEquals(5, mine.getPublishedScore());
    }

    @Test
    void eligibilityPublishesOverdueContentBeforeReopening() {
        UserReview mine = overdueAuthoredReview();
        actorTravelledWith(NOW.minusDays(1));
        when(reviews.findByReviewerIdAndTargetUserId(1L, 2L)).thenReturn(Optional.of(mine));

        service.getEligibility(actor);

        assertEquals(5, mine.getPublishedScore());
        assertEquals("preserved", mine.getPublishedDetails());
        assertEquals(ReviewStatus.PENDING, mine.getStatus());
        assertNull(mine.getSubmittedAt());
    }

    @Test
    void deletedAuthorSubmissionPublishesAtItsDeadline() {
        UserReview mine = overdueAuthoredReview();
        mine.clearReviewer();
        mine.setReviewerName(ReviewPolicy.DELETED_MEMBER);
        when(users.existsById(2L)).thenReturn(true);
        when(reviews.findAllReceivedByTargetUserId(2L)).thenReturn(List.of(mine));

        assertEquals(1, service.getPublicReviews(2L).summary().count());
        assertEquals(ReviewStatus.PUBLISHED, mine.getStatus());
    }

    private UserReview overdueAuthoredReview() {
        UserReview mine = new UserReview(actor, target, 5, "preserved", NOW.toLocalDate(),
                ReviewAuthorRole.PASSENGER);
        ReflectionTestUtils.setField(mine, "id", 99L);
        mine.setLatestSharedDropoffAt(NOW.minusDays(20));
        mine.setWindowEndsAt(NOW.minusDays(6));
        mine.setSubmittedAt(NOW.minusDays(19));
        return mine;
    }

    @Test
    void aMemberWithoutASharedRideCannotReview() {
        when(bookings.findReviewableBookingsAsPassenger(anyLong(), any(), any())).thenReturn(List.of());
        when(bookings.findReviewableBookingsAsDriver(anyLong(), any(), any())).thenReturn(List.of());

        assertThrows(ReviewNotEligibleException.class,
                () -> service.submit(actor, new ReviewRequestDto(2L, 4, "never met")));
    }

    @Test
    void deletingAnAccountAnonymizesContributionsAndRemovesItsOwnReputation() {
        UserReview received = new UserReview(target, actor, 5, "about the leaver", LocalDate.now(),
                ReviewAuthorRole.PASSENGER);
        UserReview authored = new UserReview(actor, target, 4, "by the leaver", LocalDate.now(),
                ReviewAuthorRole.PASSENGER);
        when(reviews.findAllReceivedByTargetUserId(1L)).thenReturn(List.of(received));
        when(reviews.findAllByReviewerIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(authored));
        when(reviews.findAllReceivedByTargetUserId(2L)).thenReturn(List.of());

        service.anonymizeForDeletedUser(actor);

        verify(reviews).deleteAll(List.of(received));
        verify(reviews, never()).deleteAll(List.of(authored));
        assertNull(authored.getReviewer());
        assertEquals(ReviewPolicy.DELETED_MEMBER, authored.getReviewerName());
        verify(reviews).save(authored);
    }
}