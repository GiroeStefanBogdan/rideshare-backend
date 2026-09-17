package com.example.blablacar.service.ride;

import com.example.blablacar.exception.ride.BookingCancellationExpiredException;
import com.example.blablacar.exception.ride.BookingNotFoundException;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RideServiceCancelBookingTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T10:00Z");
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final RideRepository rides = mock(RideRepository.class);
    private final RideStopRepository stops = mock(RideStopRepository.class);
    private final User passenger = new User();
    private final RideService service = new RideService(rides, null, null, stops, bookings,
            null, Clock.fixed(NOW.toInstant(), NOW.getOffset()), null);
    private final RideStop before = stop(1, (byte) 3, NOW.minusHours(1));
    private final RideStop pickup = stop(2, (byte) 1, NOW.plusHours(1));
    private final RideStop middle = stop(3, (byte) 2, NOW.plusHours(2));
    private final RideStop dropoff = stop(4, (byte) 4, NOW.plusHours(3));
    private final Ride ride = new Ride(new User(), null, null, List.of(before, pickup, middle, dropoff),
            (byte) 4, (short) 30, before.getDepartsAt(), null);
    private final Booking booking = new Booking(passenger, ride, pickup, dropoff, (byte) 2, 40);

    private static RideStop stop(final int order, final byte capacity, final OffsetDateTime time) {
        return new RideStop(null, null, (byte) order, time, capacity, (short) (order * 10));
    }

    private void available() {
        passenger.setId(9L);
        ReflectionTestUtils.setField(ride, "id", 11L);
        when(bookings.findRideIdByIdAndPassengerId(31L, 9L)).thenReturn(Optional.of(11L));
        when(rides.findByIdForUpdate(11L)).thenReturn(Optional.of(ride));
        when(bookings.findByIdForUpdate(31L)).thenReturn(Optional.of(booking));
        when(stops.findAllByRide(ride)).thenReturn(ride.getRideStops());
    }

    @Test
    void onlyThisBookingsSeatsAndSegmentsAreRestoredExactlyOnce() {
        available();
        service.cancelBooking(passenger, 31L);
        assertEquals((byte) 3, before.getAvailableSeats());
        assertEquals((byte) 3, pickup.getAvailableSeats());
        assertEquals((byte) 4, middle.getAvailableSeats());
        assertEquals((byte) 4, dropoff.getAvailableSeats());
        assertEquals(Status.CANCELLED, booking.getStatus());
        service.cancelBooking(passenger, 31L);
        assertEquals((byte) 3, pickup.getAvailableSeats());
    }

    @Test
    void missingBookingIsNotFound() {
        assertThrows(BookingNotFoundException.class, () -> service.cancelBooking(passenger, 99L));
    }

    @Test
    void otherPassengerCannotCancel() {
        available();
        User other = new User();
        other.setId(10L);
        assertThrows(BookingNotFoundException.class, () -> service.cancelBooking(other, 31L));
        assertEquals((byte) 1, pickup.getAvailableSeats());
    }

    @Test
    void atOrAfterPickupAndMissingTimeAreRejected() {
        available();
        for (OffsetDateTime time : List.of(NOW, NOW.minusSeconds(1))) {
            ReflectionTestUtils.setField(pickup, "departsAt", time);
            assertThrows(BookingCancellationExpiredException.class, () -> service.cancelBooking(passenger, 31L));
        }
        ReflectionTestUtils.setField(pickup, "departsAt", null);
        assertThrows(BookingCancellationExpiredException.class, () -> service.cancelBooking(passenger, 31L));
        assertEquals(Status.ACTIVE, booking.getStatus());
    }

    @Test
    void cancelledRideDoesNotReleaseCapacity() {
        available();
        ride.setStatus(Status.CANCELLED);
        service.cancelBooking(passenger, 31L);
        assertEquals((byte) 1, pickup.getAvailableSeats());
    }
}
