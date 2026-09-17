package com.example.blablacar.dto.ride;

import java.util.List;

public record RideDetailsDTO(
        long rideId,
        RideDriverDTO driver,
        Byte seatsTotal,
        RideVehicleDTO vehicle,
        List<RideStopDetailsDTO> rideStops
) {
}
