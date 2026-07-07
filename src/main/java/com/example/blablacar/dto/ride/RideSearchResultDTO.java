package com.example.blablacar.dto.ride;

/**
 * Matches the frontend's {@code RideSearchResult} interface exactly.
 */
public record RideSearchResultDTO(
        long rideId,
        RideDriverDTO driver,
        Integer seatsAvailable,
        int totalPrice,
        RideStopBasicDTO startStop,
        RideStopBasicDTO endStop,
        double distanceToStartKm,
        double distanceToEndKm
) {
}
