package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.MyRidesResponseDTO;
import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RideServiceMyRidesTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T10:00Z");
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final RideRepository rides = mock(RideRepository.class);
    private final User user = new User();
    private final RideService service = new RideService(rides, null, null, null, bookings,
            null, Clock.fixed(NOW.toInstant(), NOW.getOffset()), null);

    private MyRidesResponseDTO response(final OffsetDateTime end, final Status status) {
        user.setId(7L);
        AdministrativeUnit location = new AdministrativeUnit();
        location.setName("Cluj");
        RideStop start = new RideStop(location, null, (byte) 1, NOW.minusHours(2), (byte) 2, (short) 0);
        RideStop finish = new RideStop(location, null, (byte) 2, end, (byte) 4, (short) 30);
        ReflectionTestUtils.setField(start, "id", 1L);
        ReflectionTestUtils.setField(finish, "id", 2L);
        Ride ride = new Ride(user, location, location, List.of(finish, start), (byte) 4,
                (short) 30, start.getDepartsAt(), null);
        ReflectionTestUtils.setField(ride, "id", 11L);
        ride.setStatus(status);
        Booking booking = new Booking(user, ride, start, finish, (byte) 2, 60);
        ReflectionTestUtils.setField(booking, "id", 31L);
        when(bookings.findRecentByPassengerId(7L, NOW.minusMonths(1))).thenReturn(List.of(booking));
        when(rides.findRecentByDriverId(7L, NOW.minusMonths(1))).thenReturn(List.of(ride));
        return service.getMyRides(user);
    }

    @Test
    void emptyAccount() {
        MyRidesResponseDTO result = service.getMyRides(user);
        assertEquals(List.of(), result.upcomingBookings());
        assertEquals(List.of(), result.pastBookings());
        assertEquals(List.of(), result.upcomingHostedRides());
        assertEquals(List.of(), result.pastHostedRides());
    }

    @Test
    void underwayStaysUpcomingAndStopsAreOrdered() {
        MyRidesResponseDTO result = response(NOW.plusSeconds(1), Status.ACTIVE);
        assertEquals(1, result.upcomingBookings().size());
        assertEquals(1, result.upcomingHostedRides().size());
        assertEquals(1, result.upcomingHostedRides().getFirst().rideStops().getFirst().stopOrder());
        assertEquals(60, result.upcomingBookings().getFirst().totalPrice());
    }

    @Test
    void exactEndIsPastForBothRoles() {
        MyRidesResponseDTO result = response(NOW, Status.ACTIVE);
        assertEquals(0, result.upcomingBookings().size());
        assertEquals(0, result.upcomingHostedRides().size());
        assertEquals(1, result.pastBookings().size());
        assertEquals(1, result.pastHostedRides().size());
    }

    @Test
    void monthEdgeIsInclusiveAndOlderIsOmitted() {
        assertEquals(1, response(NOW.minusMonths(1), Status.ACTIVE).pastBookings().size());
        MyRidesResponseDTO older = response(NOW.minusMonths(1).minusNanos(1), Status.ACTIVE);
        assertEquals(0, older.pastBookings().size());
        assertEquals(0, older.upcomingBookings().size());
        assertEquals(0, older.pastHostedRides().size());
    }

    @Test
    void offsetsCompareByInstant() {
        assertEquals(1, response(NOW.withOffsetSameInstant(java.time.ZoneOffset.ofHours(3)),
                Status.ACTIVE).pastBookings().size());
    }

    @Test
    void cancelledAndLegacyRideStatusesPropagateToBookings() {
        for (Status status : List.of(Status.CANCELLED, Status.INACTIVE)) {
            MyRidesResponseDTO result = response(NOW.plusHours(1), status);
            assertEquals(Status.CANCELLED, result.upcomingBookings().getFirst().status());
            assertEquals(Status.CANCELLED, result.upcomingHostedRides().getFirst().status());
        }
    }

    @Test
    void missingLegacyEndRemainsVisible() {
        assertEquals(1, response(null, Status.ACTIVE).upcomingBookings().size());
    }
}
