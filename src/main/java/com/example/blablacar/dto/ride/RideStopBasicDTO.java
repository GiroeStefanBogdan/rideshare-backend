package com.example.blablacar.dto.ride;

/**
 * Matches the frontend's {@code RideStopBasic} interface.
 */
public record RideStopBasicDTO(
        long id,
        String locationName,
        String departsAt     // ISO 8601
) {
}
