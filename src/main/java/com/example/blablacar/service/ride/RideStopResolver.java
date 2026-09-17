package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.RideStopDTO;
import com.example.blablacar.exception.ride.InvalidRidePricingException;
import com.example.blablacar.exception.ride.InvalidRideScheduleException;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RideStopResolver {

    private final StreetRepository streetRepository;
    private final AdministrativeUnitRepository administrativeUnitRepository;

    public RideStopResolver(final StreetRepository streetRepository,
                            final AdministrativeUnitRepository administrativeUnitRepository) {
        this.streetRepository = streetRepository;
        this.administrativeUnitRepository = administrativeUnitRepository;
    }

    public List<RideStop> resolve(final List<RideStopDTO> requestedStops, final byte seatsTotal) {
        List<RideStopDTO> rideStops = requestedStops.stream()
                .sorted(Comparator.comparing(RideStopDTO::stopOrder))
                .toList();
        if (rideStops.stream().map(RideStopDTO::id).distinct().count() != rideStops.size()) {
            throw new InvalidRideStopException("Ride locations must be distinct");
        }
        validateOrderAndSchedule(rideStops);
        validatePricing(rideStops);

        Map<Boolean, List<RideStopDTO>> groupedStops = rideStops.stream()
                .collect(Collectors.groupingBy(stop -> "STREET".equals(stop.type())));
        List<RideStopDTO> streetStops = groupedStops.getOrDefault(Boolean.TRUE, List.of());
        List<Street> foundStreets = streetRepository.findAllById(
                streetStops.stream().map(RideStopDTO::id).toList());
        if (foundStreets.size() != streetStops.size()) {
            throw new InvalidRideStopException();
        }

        Set<Long> administrativeUnitStopIds = groupedStops.getOrDefault(Boolean.FALSE, List.of()).stream()
                .map(RideStopDTO::id)
                .collect(Collectors.toCollection(HashSet::new));
        foundStreets.forEach(street -> administrativeUnitStopIds.add(street.getLocation().getId()));

        List<AdministrativeUnit> foundAdministrativeUnits = administrativeUnitRepository.findAllById(
                administrativeUnitStopIds);
        if (foundAdministrativeUnits.size() != administrativeUnitStopIds.size()) {
            throw new InvalidRideStopException();
        }

        Map<Long, AdministrativeUnit> administrativeUnitsById = foundAdministrativeUnits.stream()
                .collect(Collectors.toMap(AdministrativeUnit::getId, unit -> unit));
        Map<Long, Street> streetsById = foundStreets.stream()
                .collect(Collectors.toMap(Street::getId, street -> street));

        return rideStops.stream().map(stop -> {
            Street street = null;
            AdministrativeUnit administrativeUnit;
            if ("STREET".equals(stop.type())) {
                street = streetsById.get(stop.id());
                administrativeUnit = administrativeUnitsById.get(street.getLocation().getId());
            } else {
                administrativeUnit = administrativeUnitsById.get(stop.id());
            }
            return new RideStop(administrativeUnit, street, stop.stopOrder(), stop.departsAt(), seatsTotal,
                    stop.cumulativePricePerSeat());
        }).toList();
    }

    private void validateOrderAndSchedule(final List<RideStopDTO> stops) {
        for (int index = 0; index < stops.size(); index++) {
            RideStopDTO stop = stops.get(index);
            if (stop.stopOrder() != index + 1) {
                throw new InvalidRideScheduleException("Stop order must be contiguous starting at 1");
            }
            if (index > 0) {
                OffsetDateTime previousTime = stops.get(index - 1).departsAt();
                if (Duration.between(previousTime, stop.departsAt()).toMinutes() < 1) {
                    throw new InvalidRideScheduleException("Stop times must be at least one minute apart");
                }
            }
        }
    }

    private void validatePricing(final List<RideStopDTO> stops) {
        if (stops.getFirst().cumulativePricePerSeat() != 0) {
            throw new InvalidRidePricingException("The origin cumulative price must be zero");
        }
        for (int index = 1; index < stops.size(); index++) {
            if (stops.get(index).cumulativePricePerSeat()
                    <= stops.get(index - 1).cumulativePricePerSeat()) {
                throw new InvalidRidePricingException("Cumulative prices must strictly increase");
            }
        }
    }
}
