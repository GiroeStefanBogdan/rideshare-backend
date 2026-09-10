package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record RideDTO(@NotNull @Size(min = 2, max = 7,
                              message = "There should be between 2 and 7 stops")
                      List<@Valid RideStopDTO> rideStops,
                      @Min(value = 1, message = "Seats should not be less then 1")
                      @Max(value = 4, message = "Seats should not be more then 4")
                      @NotNull Byte seatsTotal,
                      Long carId
) {
}
