package com.example.blablacar.service.ride;

import com.example.blablacar.dto.review.RatingSummaryDto;
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
import com.example.blablacar.dto.ride.RideVehicleDTO;
import com.example.blablacar.exception.ride.BookingCancellationExpiredException;
import com.example.blablacar.exception.ride.BookingNotFoundException;
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
import com.example.blablacar.model.location.LocationType;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.model.user.UserCar;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import com.example.blablacar.repository.user.car.UserCarRepository;
import com.example.blablacar.service.user.ReviewService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final UserCarRepository userCarRepository;
    private final ReviewService reviewService;

    @Autowired
    public RideService(final RideRepository rideRepository,
                       final AdministrativeUnitRepository administrativeUnitRepository,
                       final StreetRepository streetRepository,
                       final RideStopRepository rideStopRepository,
                       final BookingRepository bookingRepository,
                       final RideStopResolver rideStopResolver,
                       final Clock clock,
                       final UserCarRepository userCarRepository,
                       final ReviewService reviewService) {
        this.rideRepository = rideRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
        this.streetRepository = streetRepository;
        this.rideStopRepository = rideStopRepository;
        this.rideStopResolver = rideStopResolver;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.userCarRepository = userCarRepository;
        this.reviewService = reviewService;
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
        UserCar car = rideRequest.carId() == null ? null : userCarRepository
                .findByIdAndUser_Id(rideRequest.carId(), user.getId())
                .orElseThrow(() -> new InvalidRideStopException("The selected car does not belong to the driver"));
        Ride ride = new Ride(user, rideStops.getFirst().getLocation(), rideStops.getLast().getLocation(), rideStops,
                rideRequest.seatsTotal(), rideStops.getLast().getCumulativePricePerSeat(), departureAt, car);
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
        ride.setStatus(Status.CANCELLED);
        rideRepository.save(ride);
    }

    /**
     * Cancels one booking of the given passenger, restoring exactly the seats
     * that this booking reserved on its own segments. Repeated cancellation is
     * a harmless no-op: seats are never released twice. A cancelled or inactive
     * ride already released nothing for this booking, so it is also a no-op.
     */
    @Transactional
    public void cancelBooking(final User passenger, final long bookingId) {
        // Read only the scalar ID before locking: do not cache stale booking/ride entities.
        long rideId = bookingRepository.findRideIdByIdAndPassengerId(bookingId, passenger.getId())
                .orElseThrow(BookingNotFoundException::new);
        Ride ride = rideRepository.findByIdForUpdate(rideId).orElseThrow(BookingNotFoundException::new);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(BookingNotFoundException::new);
        if (booking.getPassenger().getId() != passenger.getId()) {
            throw new ForbiddenRideException();
        }
        if (booking.getStatus() != Status.ACTIVE || ride.getStatus() != Status.ACTIVE) {
            return;
        }
        RideStop fromStop = booking.getFromStop();
        if (fromStop.getDepartsAt() == null || !fromStop.getDepartsAt()
                .isAfter(OffsetDateTime.now(clock))) {
            throw new BookingCancellationExpiredException();
        }
        List<RideStop> rideStops = rideStopRepository.findAllByRide(ride);
        byte fromOrder = fromStop.getStopOrder();
        byte toOrder = booking.getToStop().getStopOrder();
        byte seats = booking.getSeats();
        rideStops.stream()
                .filter(stop -> stop.getStopOrder() >= fromOrder && stop.getStopOrder() < toOrder)
                .forEach(stop -> stop.setAvailableSeats((byte) (stop.getAvailableSeats() + seats)));
        booking.setStatus(Status.CANCELLED);
        bookingRepository.save(booking);
    }

    @Transactional
    public List<RideSearchResultDTO> searchRides(final RideSearchRequestDTO request) {
        double[] fromCoords = resolveCoordinates(request.fromId(), request.fromType());
        double[] toCoords = resolveCoordinates(request.toId(), request.toType());

        if (fromCoords == null || toCoords == null) {
            return List.of();
        }

        Map<Long, RatingSummaryDto> summaries = new HashMap<>();
        return rideRepository.searchRides(request, fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]).stream()
                .map(result -> {
                    RideDriverDTO driver = result.driver();
                    RatingSummaryDto summary = summaries.computeIfAbsent(driver.id(), reviewService::getRatingSummary);
                    RideDriverDTO ratedDriver = new RideDriverDTO(driver.id(), driver.name(), driver.avatarUrl(),
                            summary.average(), summary.count(), driver.smokingAllowed(), driver.petFriendly());
                    return new RideSearchResultDTO(result.rideId(), ratedDriver, result.seatsAvailable(),
                            result.totalPrice(), result.startStop(), result.endStop(),
                            result.distanceToStartKm(), result.distanceToEndKm());
                }).toList();
    }

    @Transactional
    public RideDetailsDTO getRideDetails(final long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(RideNotFoundException::new);
        List<RideStopDetailsDTO> stops = ride.getRideStops().stream()
                .sorted(Comparator.comparing(RideStop::getStopOrder))
                .map(this::mapDetailedStop)
                .toList();
        return new RideDetailsDTO(ride.getId(), mapDriver(ride.getDriver(), new HashMap<>()), ride.getSeatsTotal(),
                mapVehicle(ride.getCar()), stops);
    }

    @Transactional
    public MyRidesResponseDTO getMyRides(final User user) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime monthAgo = now.minusMonths(1L);
        List<Booking> bookings = bookingRepository.findRecentByPassengerId(user.getId(), monthAgo);
        List<Ride> rides = rideRepository.findRecentByDriverId(user.getId(), monthAgo);
        Comparator<OffsetDateTime> ascending = Comparator.nullsLast(
                Comparator.comparing(OffsetDateTime::toInstant));
        Comparator<OffsetDateTime> descending = Comparator.nullsLast(
                Comparator.comparing(OffsetDateTime::toInstant).reversed());
        Map<Long, RatingSummaryDto> summaries = new HashMap<>();
        return new MyRidesResponseDTO(
                bookings.stream().filter(b -> isUpcoming(b.getToStop().getDepartsAt(), now))
                        .sorted(Comparator.comparing((Booking b) -> b.getFromStop().getDepartsAt(), ascending)
                                .thenComparing(Booking::getId)).map(b -> mapBooking(b, summaries)).toList(),
                bookings.stream().filter(b -> isPast(b.getToStop().getDepartsAt(), monthAgo, now))
                        .sorted(Comparator.comparing((Booking b) -> b.getToStop().getDepartsAt(), descending)
                                .thenComparing(Booking::getId)).map(b -> mapBooking(b, summaries)).toList(),
                rides.stream().filter(r -> isUpcoming(scheduledEnd(r), now))
                        .sorted(Comparator.comparing(Ride::getDepartureAt, ascending).thenComparing(Ride::getId))
                        .map(this::mapHostedRide).toList(),
                rides.stream().filter(r -> isPast(scheduledEnd(r), monthAgo, now))
                        .sorted(Comparator.comparing(this::scheduledEnd, descending).thenComparing(Ride::getId))
                        .map(this::mapHostedRide).toList()
        );
    }

    private OffsetDateTime scheduledEnd(final Ride ride) {
        RideStop last = ride.getRideStops().stream().max(Comparator.comparing(RideStop::getStopOrder))
                .orElse(null);
        return last == null ? null : last.getDepartsAt();
    }

    private boolean isUpcoming(final OffsetDateTime end, final OffsetDateTime now) {
        return end == null || end.isAfter(now);
    }

    private boolean isPast(final OffsetDateTime end, final OffsetDateTime from, final OffsetDateTime now) {
        return end != null && !end.isBefore(from) && !end.isAfter(now);
    }

    private double[] resolveCoordinates(final Long id, final LocationType type) {
        if (LocationType.STREET.equals(type)) {
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
        List<RideStop> rideStops = rideStopRepository.findAllByRide(ride);
        RideStop fromStop = rideStops.stream()
                .filter(s -> s.getId().equals(request.fromStopId()))
                .findFirst()
                .orElseThrow(InvalidRideStopException::new);
        RideStop toStop = rideStops.stream()
                .filter(s -> s.getId().equals(request.toStopId()))
                .findFirst()
                .orElseThrow(InvalidRideStopException::new);
        if (!fromStop.getDepartsAt().isAfter(OffsetDateTime.now(clock))) {
            throw new RideDepartedException();
        }
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
        int fromPrice = fromStop.getCumulativePricePerSeat();
        int toPrice = toStop.getCumulativePricePerSeat();
        if (toPrice <= fromPrice) {
            throw new InvalidRidePricingException("The selected segment has invalid pricing");
        }
        int totalPrice = (toPrice - fromPrice) * request.seats();
        Booking booking = new Booking(passenger, ride, fromStop, toStop, request.seats(), totalPrice);
        return bookingRepository.save(booking).getId();
    }

    private BookedRideDTO mapBooking(final Booking booking, final Map<Long, RatingSummaryDto> summaries) {
        Ride ride = booking.getRide();
        boolean cancelled = booking.getStatus() != Status.ACTIVE || ride.getStatus() != Status.ACTIVE;
        Status status = cancelled ? Status.CANCELLED : Status.ACTIVE;
        return new BookedRideDTO(
                booking.getId(),
                ride.getId(),
                status,
                mapDriver(ride.getDriver(), summaries),
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
        return new HostedRideDTO(ride.getId(),
                ride.getStatus() == Status.ACTIVE ? Status.ACTIVE : Status.CANCELLED,
                ride.getSeatsTotal(), stops);
    }

    private RideDriverDTO mapDriver(final User driver, final Map<Long, RatingSummaryDto> summaries) {
        UserInfo userInfo = driver.getUserInfo();
        RatingSummaryDto summary = summaries.computeIfAbsent(driver.getId(), reviewService::getRatingSummary);
        boolean smokingAllowed = userInfo != null && userInfo.isCanSmoke();
        boolean petFriendly = userInfo != null && userInfo.isPetFriendly();
        return new RideDriverDTO(driver.getId(), driver.getName(), null, summary.average(), summary.count(),
                smokingAllowed, petFriendly);
    }

    private RideStopBasicDTO mapBasicStop(final RideStop stop) {
        return new RideStopBasicDTO(stop.getId(), getLocationName(stop), stop.getLocation().getName(),
                toIsoString(stop.getDepartsAt()));
    }

    private RideStopDetailsDTO mapDetailedStop(final RideStop stop) {
        return new RideStopDetailsDTO(stop.getId(), stop.getStopOrder(), getLocationName(stop),
                stop.getLocation().getName(),
                toIsoString(stop.getDepartsAt()), stop.getAvailableSeats(),
                stop.getCumulativePricePerSeat());
    }

    private RideVehicleDTO mapVehicle(final UserCar car) {
        return car == null ? null : new RideVehicleDTO(car.getId(), car.getBrand(), car.getModel(), car.getColor(),
                car.getYear());
    }

    private String getLocationName(final RideStop stop) {
        return stop.getStreet() == null ? stop.getLocation().getName() : stop.getStreet().getName();
    }

    private String toIsoString(final OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
