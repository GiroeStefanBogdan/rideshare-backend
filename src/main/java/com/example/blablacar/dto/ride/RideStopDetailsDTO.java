package com.example.blablacar.dto.ride;

public record RideStopDetailsDTO(
        long id,
        byte stopOrder,
        String locationName,
        String municipalityName,
        String departsAt,
        Byte availableSeats,
        Short pricePerSeat
) {
}
