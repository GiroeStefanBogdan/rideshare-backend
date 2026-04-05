package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.RideDTO;
import com.example.blablacar.dto.ride.RideStopDTO;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.exception.ride.RideDateTooDistantException;
import com.example.blablacar.exception.ride.RideNotFoundException;
import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.AdministrativeUnitType;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    public RideService(final RideRepository rideRepository,
                       final AdministrativeUnitRepository administrativeUnitRepository,
                       final StreetRepository streetRepository, final RideStopRepository rideStopRepository) {
        this.rideRepository = rideRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
        this.streetRepository = streetRepository;
        this.rideStopRepository = rideStopRepository;
    }

    public void save(final User user, final RideDTO rideRequest) {
        if (OffsetDateTime.now().plusMonths(1L).isBefore(rideRequest.departureAt())) {
            throw new RideDateTooDistantException();
        }
        Map<Boolean, List<RideStopDTO>> collect = rideRequest.rideStops().stream()
                .collect(Collectors.groupingBy(r -> AdministrativeUnitType.STREET.equals(
                        r.type())));
        List<RideStopDTO> streetsStops = collect.get(Boolean.TRUE);
        List<Street> foundStreets = streetRepository.findAllById(streetsStops.stream().map(RideStopDTO::id).toList());
        if (foundStreets.size() != streetsStops.size()) {
            throw new InvalidRideStopException();
        }
        List<RideStopDTO> adminUnitStops = collect.get(Boolean.FALSE);

        List<AdministrativeUnit> foundAdminUnits =
                administrativeUnitRepository.findAllById(adminUnitStops.stream().map(RideStopDTO::id).toList());
        if (foundAdminUnits.size() != adminUnitStops.size()) {
            throw new InvalidRideStopException();
        }
        Map<Long, AdministrativeUnit> idToAdminUnit =
                foundAdminUnits.stream().collect(Collectors.toMap(AdministrativeUnit::getId, a -> a));
        Map<Long, Street> idToStreet =
                foundStreets.stream().collect(Collectors.toMap(Street::getId, a -> a));
        List<RideStop> rideStops = rideRequest.rideStops().stream().map(stop -> {
            Street stopStreet = null;
            AdministrativeUnit stopAdminUnit;
            if (stop.type().equals(AdministrativeUnitType.STREET)) {
                stopStreet = idToStreet.get(stop.id());
                stopAdminUnit = idToAdminUnit.get(stopStreet.getLocation().getId());
            } else {
                stopAdminUnit = idToAdminUnit.get(stop.id());
            }
            return new RideStop(stopAdminUnit, stopStreet, stop.stopOrder(), null, rideRequest.seatsTotal(),
                    stop.price());
        }).toList();
        Ride ride = new Ride(user, rideStops.getFirst().getLocation(), rideStops.getLast().getLocation(), rideStops,
                rideRequest.seatsTotal(), rideRequest.pricePerSeat(), rideRequest.departureAt());
        rideRepository.save(ride);
    }

    public void updateSeatNumber(final User user, final long rideId, final byte seatNumber) {
        Ride ride = rideRepository.findByIdForUpdate(rideId).orElseThrow(RideNotFoundException::new);
        if (ride.getDriver().getId() != user.getId()) {
            throw new ForbiddenRideException();
        }
        List<RideStop> rideStops = rideStopRepository.findAllByRide(ride);
        int difference;
        if (seatNumber < ride.getSeatsTotal()) {
            difference = ride.getSeatsTotal() - seatNumber;
            boolean canModifySeats = rideStops.stream().anyMatch(stop -> stop.getAvailableSeats() < difference);
            if (!canModifySeats) {
                throw new InvalidRideStopException();
            }
        } else {
            difference = seatNumber - ride.getSeatsTotal();
        }
        rideStops.forEach(stop -> stop.setAvailableSeats((byte) (stop.getAvailableSeats() + difference)));
        ride.setSeatsTotal(seatNumber);
        rideRepository.save(ride);
    }

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
}
