package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record RideDTO(@NotNull @Size(min = 2, message = "There should be at least 2 stops") List<RideStopDTO> rideStops,
                      @Min(value = 1, message = "Seats should not be less then 1")
                      @Max(value = 9, message = "Seats should not be more then 9")
                      @NotNull Byte seatsTotal
) {
}
