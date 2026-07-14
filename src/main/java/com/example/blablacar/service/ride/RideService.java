package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.BookedRideDTO;
import com.example.blablacar.dto.ride.HostedRideDTO;
import com.example.blablacar.dto.ride.MyRidesResponseDTO;
import com.example.blablacar.dto.ride.ReserveRideRequestDTO;
import com.example.blablacar.dto.ride.RideDTO;
import com.example.blablacar.dto.ride.RideDetailsDTO;
import com.example.blablacar.dto.ride.RideDriverDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.dto.ride.RideStopBasicDTO;
import com.example.blablacar.dto.ride.RideStopDetailsDTO;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.exception.ride.InvalidRidePricingException;
import com.example.blablacar.exception.ride.InvalidRideScheduleException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.exception.ride.NotEnoughSeatsException;
import com.example.blablacar.exception.ride.RideDateTooDistantException;
import com.example.blablacar.exception.ride.RideDepartedException;
import com.example.blablacar.exception.ride.RideInactiveException;
import com.example.blablacar.exception.ride.RideNotFoundException;
import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final AdministrativeUnitRepository administrativeUnitRepository;
    private final StreetRepository streetRepository;
    private final RideStopRepository rideStopRepository;
    private final BookingRepository bookingRepository;
    private final RideStopResolver rideStopResolver;
    private final Clock clock;

    @Autowired
    public RideService(final RideRepository rideRepository,
                       final AdministrativeUnitRepository administrativeUnitRepository,
                       final StreetRepository streetRepository,
                       final RideStopRepository rideStopRepository,
                       final BookingRepository bookingRepository,
                       final RideStopResolver rideStopResolver,
                       final Clock clock) {
        this.rideRepository = rideRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
        this.streetRepository = streetRepository;
        this.rideStopRepository = rideStopRepository;
        this.rideStopResolver = rideStopResolver;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    public Long save(final User user, final RideDTO rideRequest) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<RideStop> rideStops = rideStopResolver.resolve(rideRequest.rideStops(), rideRequest.seatsTotal());
        OffsetDateTime departureAt = rideStops.getFirst().getDepartsAt();
        if (!departureAt.isAfter(now)) {
            throw new InvalidRideScheduleException("The first stop must be in the future");
        }
        if (now.plusMonths(1L).isBefore(departureAt)) {
            throw new RideDateTooDistantException();
        }
        Ride ride = new Ride(user, rideStops.getFirst().getLocation(), rideStops.getLast().getLocation(), rideStops,
                rideRequest.seatsTotal(), rideStops.getFirst().getPricePerSeat(), departureAt);
        rideStops.forEach(rs -> rs.setRide(ride));
        return rideRepository.save(ride).getId();
    }

    @Transactional
    public void updateSeatNumber(final User user, final long rideId, final byte seatNumber) {
        Ride ride = rideRepository.findByIdForUpdate(rideId).orElseThrow(RideNotFoundException::new);
        if (ride.getDriver().getId() != user.getId()) {
            throw new ForbiddenRideException();
        }
        List<RideStop> rideStops = rideStopRepository.findAllByRide(ride);
        int difference;
        if (seatNumber < ride.getSeatsTotal()) {
            difference = ride.getSeatsTotal() - seatNumber;
            boolean cantModifySeats = rideStops.stream().anyMatch(stop -> stop.getAvailableSeats() < difference);
            if (cantModifySeats) {
                throw new InvalidRideStopException();
            }
        } else {
            difference = seatNumber - ride.getSeatsTotal();
        }
        rideStops.forEach(stop -> stop.setAvailableSeats((byte) (stop.getAvailableSeats() + difference)));
        ride.setSeatsTotal(seatNumber);
        rideRepository.save(ride);
    }

    @Transactional
    public void deleteRide(final User user, final long rideId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId).orElseThrow(RideNotFoundException::new);
        if (ride.getDriver().getId() != user.getId()) {
            throw new ForbiddenRideException();
        }
        //TODO Add notification to users in the future.
        // Preserve stops and bookings so cancelled rides remain visible in personal history.
        ride.setStatus(Status.INACTIVE);
        rideRepository.save(ride);
    }

    @Transactional
    public List<RideSearchResultDTO> searchRides(final RideSearchRequestDTO request) {
        double[] fromCoords = resolveCoordinates(request.fromId(), request.fromType());
        double[] toCoords = resolveCoordinates(request.toId(), request.toType());

        if (fromCoords == null || toCoords == null) {
            return List.of();
        }

        return rideRepository.searchRides(request, fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);
    }

    @Transactional
    public RideDetailsDTO getRideDetails(final long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(RideNotFoundException::new);
        List<RideStopDetailsDTO> stops = ride.getRideStops().stream()
                .sorted(Comparator.comparing(RideStop::getStopOrder))
                .map(this::mapDetailedStop)
                .toList();
        return new RideDetailsDTO(ride.getId(), mapDriver(ride.getDriver()), ride.getSeatsTotal(), stops);
    }

    @Transactional
    public MyRidesResponseDTO getMyRides(final User user) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime monthAgo = now.minusMonths(1L);
        List<Booking> upcomingBookings = bookingRepository.findUpcomingByPassengerId(user.getId(), now);
        List<Booking> pastBookings = bookingRepository.findPastByPassengerId(user.getId(), monthAgo, now);
        List<Ride> upcomingHostedRides = rideRepository.findUpcomingByDriverId(user.getId(), now);
        List<Ride> pastHostedRides = rideRepository.findPastByDriverId(user.getId(), monthAgo, now);
        return new MyRidesResponseDTO(
                upcomingBookings.stream().map(this::mapBooking).toList(),
                pastBookings.stream().map(this::mapBooking).toList(),
                upcomingHostedRides.stream().map(this::mapHostedRide).toList(),
                pastHostedRides.stream().map(this::mapHostedRide).toList()
        );
    }

    private double[] resolveCoordinates(final Long id, final String type) {
        if ("STREET".equals(type)) {
            return streetRepository.findById(id)
                    .map(s -> new double[] {s.getLatitude().doubleValue(), s.getLongitude().doubleValue()})
                    .orElse(null);
        }
        return administrativeUnitRepository.findById(id)
                .map(a -> new double[] {a.getLatitude().doubleValue(), a.getLongitude().doubleValue()})
                .orElse(null);
    }

    @Transactional
    public long reserveSeats(final User passenger, final long rideId,
                             final ReserveRideRequestDTO request) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(RideNotFoundException::new);
        if (ride.getStatus() != Status.ACTIVE) {
            throw new RideInactiveException();
        }
//        if (ride.getDriver().getId() == passenger.getId()) {
//            throw new ForbiddenRideException();
//        }
        if (ride.getDepartureAt().isBefore(OffsetDateTime.now(clock))) {
            throw new RideDepartedException();
        }
        List<RideStop> rideStops = rideStopRepository.findAllByRide(ride);
        RideStop fromStop = rideStops.stream()
                .filter(s -> s.getId().equals(request.fromStopId()))
                .findFirst()
                .orElseThrow(InvalidRideStopException::new);
        RideStop toStop = rideStops.stream()
                .filter(s -> s.getId().equals(request.toStopId()))
                .findFirst()
                .orElseThrow(InvalidRideStopException::new);
        if (fromStop.getStopOrder() >= toStop.getStopOrder()) {
            throw new InvalidRideStopException();
        }
        byte fromOrder = fromStop.getStopOrder();
        byte toOrder = toStop.getStopOrder();
        List<RideStop> segmentStops = rideStops.stream()
                .filter(s -> s.getStopOrder() >= fromOrder && s.getStopOrder() < toOrder)
                .toList();
        for (RideStop stop : segmentStops) {
            if (stop.getAvailableSeats() < request.seats()) {
                throw new NotEnoughSeatsException();
            }
        }
        for (RideStop stop : segmentStops) {
            stop.setAvailableSeats((byte) (stop.getAvailableSeats() - request.seats()));
        }
        int fromPrice = fromStop.getPricePerSeat() != null ? fromStop.getPricePerSeat() : 0;
        int toPrice = toStop.getPricePerSeat() != null ? toStop.getPricePerSeat() : 0;
        if (fromPrice < toPrice) {
            throw new InvalidRidePricingException("The selected segment has invalid pricing");
        }
        int totalPrice = (fromPrice - toPrice) * request.seats();
        Booking booking = new Booking(passenger, ride, fromStop, toStop, request.seats(), totalPrice);
        return bookingRepository.save(booking).getId();
    }

    private BookedRideDTO mapBooking(final Booking booking) {
        Ride ride = booking.getRide();
        Status status = booking.getStatus() == Status.INACTIVE || ride.getStatus() == Status.INACTIVE
                ? Status.INACTIVE : Status.ACTIVE;
        return new BookedRideDTO(
                booking.getId(),
                ride.getId(),
                status,
                mapDriver(ride.getDriver()),
                booking.getSeats(),
                booking.getTotalPrice(),
                mapBasicStop(booking.getFromStop()),
                mapBasicStop(booking.getToStop())
        );
    }

    private HostedRideDTO mapHostedRide(final Ride ride) {
        List<RideStopDetailsDTO> stops = ride.getRideStops().stream()
                .sorted(Comparator.comparing(RideStop::getStopOrder))
                .map(this::mapDetailedStop)
                .toList();
        return new HostedRideDTO(ride.getId(), ride.getStatus(), ride.getSeatsTotal(), stops);
    }

    private RideDriverDTO mapDriver(final User driver) {
        UserInfo userInfo = driver.getUserInfo();
        Double rating = userInfo == null || userInfo.getRating() == null
                ? Double.valueOf(0.0) : userInfo.getRating();
        Integer reviewsCount = userInfo == null || userInfo.getReviewsCount() == null
                ? Integer.valueOf(0) : userInfo.getReviewsCount();
        boolean smokingAllowed = userInfo != null && userInfo.isCanSmoke();
        boolean petFriendly = userInfo != null && userInfo.isPetFriendly();
        return new RideDriverDTO(driver.getId(), driver.getName(), null, rating, reviewsCount,
                smokingAllowed, petFriendly);
    }

    private RideStopBasicDTO mapBasicStop(final RideStop stop) {
        return new RideStopBasicDTO(stop.getId(), getLocationName(stop), stop.getLocation().getName(),
                toIsoString(stop.getDepartsAt()));
    }

    private RideStopDetailsDTO mapDetailedStop(final RideStop stop) {
        return new RideStopDetailsDTO(stop.getId(), stop.getStopOrder(), getLocationName(stop),
                stop.getLocation().getName(),
                toIsoString(stop.getDepartsAt()), stop.getAvailableSeats(), stop.getPricePerSeat());
    }

    private String getLocationName(final RideStop stop) {
        return stop.getStreet() == null ? stop.getLocation().getName() : stop.getStreet().getName();
    }

    private String toIsoString(final OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
