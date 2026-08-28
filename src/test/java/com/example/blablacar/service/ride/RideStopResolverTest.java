package com.example.blablacar.service.ride;

import com.example.blablacar.dto.ride.RideStopDTO;
import com.example.blablacar.exception.ride.InvalidRideStopException;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import com.example.blablacar.repository.location.AdministrativeUnitRepository;
import com.example.blablacar.repository.location.StreetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideStopResolverTest {

    @Mock
    private StreetRepository streetRepository;
    @Mock
    private AdministrativeUnitRepository adminUnitRepository;

    private RideStopResolver resolver;

    private AdministrativeUnit city;
    private Street street;

    @BeforeEach
    void setUp() {
        resolver = new RideStopResolver(streetRepository, adminUnitRepository);

        city = new AdministrativeUnit();
        city.setId(1L);
        city.setName("Cluj-Napoca");

        street = new Street();
        street.setId(10L);
        street.setName("Strada Memorandumului");
        street.setLocation(city);
    }

    @Test
    void resolveShouldReturnRideStopsForValidAdminUnitStops() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1L);
        RideStopDTO stop1 = new RideStopDTO(1L, "ADMIN_UNIT", (byte) 1, (short) 10, start);
        RideStopDTO stop2 = new RideStopDTO(2L, "ADMIN_UNIT", (byte) 2, (short) 0, start.plusHours(1L));

        AdministrativeUnit city2 = new AdministrativeUnit();
        city2.setId(2L);
        city2.setName("Sibiu");

        when(streetRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(adminUnitRepository.findAllById(anyCollection())).thenReturn(List.of(city, city2));

        var result = resolver.resolve(List.of(stop1, stop2), (byte) 4);

        assertEquals(2, result.size());
        assertEquals(city, result.get(0).getLocation());
        assertEquals(city2, result.get(1).getLocation());
        assertEquals((byte) 4, result.get(0).getAvailableSeats());
    }

    @Test
    void resolveShouldThrowWhenStreetNotFound() {
        RideStopDTO stop = new RideStopDTO(999L, "STREET", (byte) 1, (short) 0,
                OffsetDateTime.now().plusDays(1L));

        when(streetRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThrows(InvalidRideStopException.class,
                () -> resolver.resolve(List.of(stop), (byte) 4));
    }

    @Test
    void resolveShouldThrowWhenAdminUnitNotFound() {
        RideStopDTO stop = new RideStopDTO(999L, "ADMIN_UNIT", (byte) 1, (short) 0,
                OffsetDateTime.now().plusDays(1L));

        when(streetRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(adminUnitRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThrows(InvalidRideStopException.class,
                () -> resolver.resolve(List.of(stop), (byte) 4));
    }

    @Test
    void resolveShouldResolveStreetStopWithParentAdminUnit() {
        RideStopDTO stop = new RideStopDTO(10L, "STREET", (byte) 1, (short) 0,
                OffsetDateTime.now().plusDays(1L));

        when(streetRepository.findAllById(anyCollection())).thenReturn(List.of(street));
        when(adminUnitRepository.findAllById(anyCollection())).thenReturn(List.of(city));

        var result = resolver.resolve(List.of(stop), (byte) 3);

        assertEquals(1, result.size());
        assertEquals(city, result.getFirst().getLocation());
        assertEquals(street, result.getFirst().getStreet());
    }
}
