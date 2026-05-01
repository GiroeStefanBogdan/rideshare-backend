package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.RideDTO;
import com.example.blablacar.dto.ride.RideDriverDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.dto.ride.RideStopBasicDTO;
import com.example.blablacar.dto.ride.RideStopDTO;
import com.example.blablacar.exception.ride.ForbiddenRideException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.exception.ride.RideDateTooDistantException;
import com.example.blablacar.exception.ride.RideNotFoundException;
import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import com.example.blablacar.repository.ride.RideRepository;
import com.example.blablacar.repository.ride.RideStopRepository;
import io.jsonwebtoken.lang.Collections;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public Long save(final User user, final RideDTO rideRequest) {
        if (OffsetDateTime.now().plusMonths(1L).isBefore(rideRequest.departureAt())) {
            throw new RideDateTooDistantException();
        }
        Map<Boolean, List<RideStopDTO>> collect = rideRequest.rideStops().stream()
                .collect(Collectors.groupingBy(r -> "STREET".equals(
                        r.type())));
        List<RideStopDTO> streetsStops = collect.get(Boolean.TRUE);
        List<Street> foundStreets = streetRepository.findAllById(streetsStops.stream().map(RideStopDTO::id).toList());
        if (foundStreets.size() != streetsStops.size()) {
            throw new InvalidRideStopException();
        }
        Set<Long> adminUnitStops =
                collect.getOrDefault(Boolean.FALSE, Collections.emptyList()).stream().map(RideStopDTO::id)
                        .collect(Collectors.toCollection(
                        HashSet::new));
        foundStreets.forEach(street -> adminUnitStops.add(street.getLocation().getId()));
        List<AdministrativeUnit> foundAdminUnits = administrativeUnitRepository.findAllById(adminUnitStops);
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
            if ("STREET".equals(stop.type())) {
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
        rideStops.forEach(rideStop -> rideStop.setRide(ride));
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

    // ──────────────────────────────────────────────────────────────
    //  Ride search
    // ──────────────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<RideSearchResultDTO> searchRides(RideSearchRequestDTO request) {
        double[] fromCoords = resolveCoordinates(request.fromId(), request.fromType());
        double[] toCoords = resolveCoordinates(request.toId(), request.toType());

        if (fromCoords == null || toCoords == null) {
            return List.of();
        }

        List<Tuple> rows = rideRepository.searchRides(request, fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);

        return rows.stream().map(this::mapRow).toList();
    }

    private double[] resolveCoordinates(Long id, String type) {
        if ("STREET".equals(type)) {
            return streetRepository.findById(id)
                    .map(s -> new double[] {s.getLatitude().doubleValue(), s.getLongitude().doubleValue()})
                    .orElse(null);
        }
        return administrativeUnitRepository.findById(id)
                .map(a -> new double[] {a.getLatitude().doubleValue(), a.getLongitude().doubleValue()})
                .orElse(null);
    }

    private RideSearchResultDTO mapRow(Tuple row) {
        long rideId = row.get("ride_id", Number.class).longValue();
        long rsFromId = row.get("rs_from_id", Number.class).longValue();
        long rsToId = row.get("rs_to_id", Number.class).longValue();
        double distStart = row.get("dist_start_km", Number.class).doubleValue();
        double distEnd = row.get("dist_end_km", Number.class).doubleValue();

        Ride ride = rideRepository.findById(rideId).orElseThrow(RideNotFoundException::new);
        RideStop fromStop = rideStopRepository.findById(rsFromId).orElseThrow();
        RideStop toStop = rideStopRepository.findById(rsToId).orElseThrow();

        User driver = ride.getDriver();
        UserInfo info = driver.getUserInfo();

        RideDriverDTO driverDTO = new RideDriverDTO(
                driver.getId(),
                driver.getName(),
                null,  // avatarUrl — not yet implemented
                (info != null && info.getRating() != null) ? info.getRating() : 0.0,
                info != null && info.getReviewsCount() != null ? info.getReviewsCount() : 0,
                info != null && info.isCanSmoke(),
                info != null && info.isPetFriendly()
        );

        RideStopBasicDTO startDTO = new RideStopBasicDTO(
                fromStop.getId(),
                fromStop.getStreet() != null
                        ? fromStop.getStreet().getName()
                        : fromStop.getLocation().getName(),
                fromStop.getDepartsAt() != null
                        ? fromStop.getDepartsAt().toString()
                        : null
        );

        RideStopBasicDTO endDTO = new RideStopBasicDTO(
                toStop.getId(),
                toStop.getStreet() != null
                        ? toStop.getStreet().getName()
                        : toStop.getLocation().getName(),
                toStop.getDepartsAt() != null
                        ? toStop.getDepartsAt().toString()
                        : null
        );

        int totalPrice = 0;
        if (fromStop.getPricePerSeat() != null) {
            int endPrice = toStop.getPricePerSeat() != null ? toStop.getPricePerSeat() : 0;
            totalPrice = fromStop.getPricePerSeat() - endPrice;
        }

        return new RideSearchResultDTO(
                ride.getId(),
                driverDTO,
                fromStop.getAvailableSeats(),
                totalPrice,
                startDTO,
                endDTO,
                distStart,
                distEnd
        );
    }
}
