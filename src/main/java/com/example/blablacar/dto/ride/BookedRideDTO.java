package com.example.blablacar.dto.ride;

import com.example.blablacar.model.enums.Status;

public record BookedRideDTO(
        long bookingId,
        long rideId,
        Status status,
        RideDriverDTO driver,
        Byte seats,
        Integer totalPrice,
        RideStopBasicDTO fromStop,
        RideStopBasicDTO toStop
) {
}
