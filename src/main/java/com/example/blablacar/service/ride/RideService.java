package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.ReserveRideRequestDTO;
import com.example.blablacar.dto.ride.RideDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.exception.ride.ForbiddenRideException;
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
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
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

    @Autowired
    public RideService(final RideRepository rideRepository,
                       final AdministrativeUnitRepository administrativeUnitRepository,
                       final StreetRepository streetRepository,
                       final RideStopRepository rideStopRepository,
                       final BookingRepository bookingRepository,
                       final RideStopResolver rideStopResolver) {
        this.rideRepository = rideRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
        this.streetRepository = streetRepository;
        this.rideStopRepository = rideStopRepository;
        this.rideStopResolver = rideStopResolver;
        this.bookingRepository = bookingRepository;
    }

    public Long save(final User user, final RideDTO rideRequest) {
        if (OffsetDateTime.now().plusMonths(1L).isBefore(rideRequest.departureAt())) {
            throw new RideDateTooDistantException();
        }
        List<RideStop> rideStops = rideStopResolver.resolve(rideRequest.rideStops(), rideRequest.seatsTotal());
        Ride ride = new Ride(user, rideStops.getFirst().getLocation(), rideStops.getLast().getLocation(), rideStops,
                rideRequest.seatsTotal(), rideRequest.pricePerSeat(), rideRequest.departureAt());
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
        rideStopRepository.deleteAllByRide(ride);
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
        if (ride.getDepartureAt().isBefore(OffsetDateTime.now())) {
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
        int totalPrice = (fromPrice - toPrice) * request.seats();
        Booking booking = new Booking(passenger, ride, fromStop, toStop, request.seats(), totalPrice);
        return bookingRepository.save(booking).getId();
    }
}
