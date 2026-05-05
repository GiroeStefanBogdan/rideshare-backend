package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.RideStopDTO;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.model.ride.RideStop;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import io.jsonwebtoken.lang.Collections;
import org.springframework.stereotype.Component;

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

    public List<RideStop> resolve(final List<RideStopDTO> rideStops, final byte seatsTotal) {
        Map<Boolean, List<RideStopDTO>> collect = rideStops.stream()
                .collect(Collectors.groupingBy(r -> "STREET".equals(r.type())));

        List<RideStopDTO> streetsStops =
                collect.getOrDefault(Boolean.TRUE, Collections.emptyList());
        List<Street> foundStreets = streetRepository.findAllById(
                streetsStops.stream().map(RideStopDTO::id).toList());
        if (foundStreets.size() != streetsStops.size()) {
            throw new InvalidRideStopException();
        }

        Set<Long> adminUnitStops =
                collect.getOrDefault(Boolean.FALSE, Collections.emptyList()).stream()
                        .map(RideStopDTO::id)
                        .collect(Collectors.toCollection(HashSet::new));
        foundStreets.forEach(street -> adminUnitStops.add(street.getLocation().getId()));

        List<AdministrativeUnit> foundAdminUnits =
                administrativeUnitRepository.findAllById(adminUnitStops);
        if (foundAdminUnits.size() != adminUnitStops.size()) {
            throw new InvalidRideStopException();
        }

        Map<Long, AdministrativeUnit> idToAdminUnit =
                foundAdminUnits.stream().collect(Collectors.toMap(AdministrativeUnit::getId, a -> a));
        Map<Long, Street> idToStreet =
                foundStreets.stream().collect(Collectors.toMap(Street::getId, a -> a));

        return rideStops.stream().map(stop -> {
            Street stopStreet = null;
            AdministrativeUnit stopAdminUnit;
            if ("STREET".equals(stop.type())) {
                stopStreet = idToStreet.get(stop.id());
                stopAdminUnit = idToAdminUnit.get(stopStreet.getLocation().getId());
            } else {
                stopAdminUnit = idToAdminUnit.get(stop.id());
            }
            return new RideStop(stopAdminUnit, stopStreet, stop.stopOrder(), null, seatsTotal,
                    stop.price());
        }).toList();
    }
}
