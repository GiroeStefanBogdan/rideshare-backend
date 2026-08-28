package com.example.blablacar.dto.ride;

import com.example.blablacar.model.enums.Status;

import java.util.List;

public record HostedRideDTO(
        long rideId,
        Status status,
        Byte seatsTotal,
        List<RideStopDetailsDTO> rideStops
) {
}
