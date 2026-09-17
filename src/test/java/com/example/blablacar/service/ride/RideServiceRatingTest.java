package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.MyRidesResponseDTO;
import com.example.blablacar.dto.ride.RideDriverDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.dto.ride.RideStopBasicDTO;
import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.LocationType;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.model.user.UserReview;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.user.ReviewRepository;
import com.example.blablacar.repository.user.UserInfoRepository;
import com.example.blablacar.repository.user.UserRepository;
import com.example.blablacar.service.user.ReviewService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RideServiceRatingTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T10:00Z");
    private final RideRepository rides = mock(RideRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final AdministrativeUnitRepository locations = mock(AdministrativeUnitRepository.class);
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final UserInfoRepository infos = mock(UserInfoRepository.class);
    private final Clock clock = Clock.fixed(NOW.toInstant(), NOW.getOffset());
    private final ReviewService reviewService = new ReviewService(reviews, mock(UserRepository.class),
            infos, bookings, clock);
    private final RideService service = new RideService(rides, locations, null, null, bookings,
            null, clock, null, reviewService);
    private final User driver = new User();
    private final User passenger = new User();

    private UserReview fixture(final boolean due) {
        driver.setId(20L);
        driver.setName("Fresh driver");
        passenger.setId(21L);
        UserReview review = new UserReview(passenger, driver, 4, "Good ride", NOW.toLocalDate(),
                ReviewAuthorRole.PASSENGER);
        review.setSubmittedAt(NOW.minusDays(2));
        review.setWindowEndsAt(NOW);
        if (!due) {
            review.setStatus(ReviewStatus.PUBLISHED);
            review.setPublishedScore(4);
            review.setPublishedAt(NOW.minusDays(1));
        }
        when(reviews.findAllReceivedByTargetUserId(20L)).thenReturn(List.of(review));
        assertNull(driver.getUserInfo());
        return review;
    }

    private Ride ride(final long id, final OffsetDateTime end) {
        AdministrativeUnit location = new AdministrativeUnit();
        location.setName("Cluj");
        RideStop start = new RideStop(location, null, (byte) 1, end.minusHours(1), (byte) 3, (short) 0);
        RideStop finish = new RideStop(location, null, (byte) 2, end, (byte) 3, (short) 30);
        ReflectionTestUtils.setField(start, "id", 1L);
        ReflectionTestUtils.setField(finish, "id", 2L);
        Ride ride = new Ride(driver, location, location, List.of(start, finish), (byte) 3,
                (short) 30, start.getDepartsAt(), null);
        ReflectionTestUtils.setField(ride, "id", id);
        return ride;
    }

    private void assertRated(final RideDriverDTO result, final UserReview review) {
        assertEquals(20L, result.id());
        assertEquals(4.0, result.rating());
        assertEquals(1, result.reviewsCount());
        assertEquals(ReviewStatus.PUBLISHED, review.getStatus());
        verify(infos, never()).save(any(UserInfo.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void detailsUseCanonicalRatingWithoutUserInfoOnFirstRead(final boolean due) {
        UserReview review = fixture(due);
        when(rides.findById(35L)).thenReturn(Optional.of(ride(35L, NOW.plusDays(1))));
        RideDriverDTO result = service.getRideDetails(35L).driver();
        assertRated(result, review);
        assertFalse(result.smokingAllowed());
        assertFalse(result.petFriendly());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void searchReplacesStaleProjectionRatingOncePerDriverOnFirstRead(final boolean due) {
        UserReview review = fixture(due);
        AdministrativeUnit location = new AdministrativeUnit();
        ReflectionTestUtils.setField(location, "latitude", BigDecimal.ONE);
        ReflectionTestUtils.setField(location, "longitude", BigDecimal.ONE);
        when(locations.findById(1L)).thenReturn(Optional.of(location));
        RideSearchRequestDTO request = new RideSearchRequestDTO(1L, LocationType.ADMIN_UNIT,
                1L, LocationType.ADMIN_UNIT, NOW.toLocalDate(), 1, null, null, null, null, null, null);
        RideDriverDTO stale = new RideDriverDTO(20L, "Fresh driver", "avatar", 0.0, 0, true, true);
        RideStopBasicDTO start = new RideStopBasicDTO(1L, "Cluj", "Cluj", NOW.toString());
        RideStopBasicDTO end = new RideStopBasicDTO(2L, "Turda", "Turda", NOW.plusHours(1).toString());
        RideSearchResultDTO row = new RideSearchResultDTO(35L, stale, 3, 30, start, end, 1.5, 2.5);
        when(rides.searchRides(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(row, row));
        List<RideSearchResultDTO> results = service.searchRides(request);
        assertEquals(2, results.size());
        for (RideSearchResultDTO result : results) {
            assertRated(result.driver(), review);
            assertEquals(new RideSearchResultDTO(35L,
                    new RideDriverDTO(20L, "Fresh driver", "avatar", 4.0, 1, true, true),
                    3, 30, start, end, 1.5, 2.5), result);
        }
        verify(reviews, times(1)).findAllReceivedByTargetUserId(20L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void myRidesShareCanonicalSummaryAcrossUpcomingAndPastBookings(final boolean due) {
        UserReview review = fixture(due);
        Ride upcoming = ride(35L, NOW.plusDays(1));
        Ride past = ride(36L, NOW.minusDays(1));
        Booking futureBooking = new Booking(passenger, upcoming, upcoming.getRideStops().getFirst(),
                upcoming.getRideStops().getLast(), (byte) 1, 30);
        Booking pastBooking = new Booking(passenger, past, past.getRideStops().getFirst(),
                past.getRideStops().getLast(), (byte) 1, 30);
        ReflectionTestUtils.setField(futureBooking, "id", 1L);
        ReflectionTestUtils.setField(pastBooking, "id", 2L);
        when(bookings.findRecentByPassengerId(21L, NOW.minusMonths(1)))
                .thenReturn(List.of(futureBooking, pastBooking));
        MyRidesResponseDTO result = service.getMyRides(passenger);
        assertRated(result.upcomingBookings().getFirst().driver(), review);
        assertRated(result.pastBookings().getFirst().driver(), review);
        verify(reviews, times(1)).findAllReceivedByTargetUserId(20L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unratedDetailsKeepOptionalPreferencesAndIgnoreStaleCache(final boolean withInfo) {
        driver.setId(20L);
        if (withInfo) {
            UserInfo info = new UserInfo();
            info.setRating(5.0);
            info.setReviewsCount(99);
            ReflectionTestUtils.setField(info, "canSmoke", true);
            ReflectionTestUtils.setField(info, "petFriendly", true);
            ReflectionTestUtils.setField(driver, "userInfo", info);
        }
        when(rides.findById(35L)).thenReturn(Optional.of(ride(35L, NOW.plusDays(1))));
        RideDriverDTO result = service.getRideDetails(35L).driver();
        assertNull(result.rating());
        assertEquals(0, result.reviewsCount());
        assertEquals(withInfo, result.smokingAllowed());
        assertEquals(withInfo, result.petFriendly());
        assertTrue(service.getMyRides(passenger).upcomingBookings().isEmpty());
    }
}
