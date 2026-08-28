package com.example.blablacar.dto.ride;

import java.util.List;

public record MyRidesResponseDTO(
        List<BookedRideDTO> upcomingBookings,
        List<BookedRideDTO> pastBookings,
        List<HostedRideDTO> upcomingHostedRides,
        List<HostedRideDTO> pastHostedRides
) {
}
