package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveRideRequestDTO(
        @NotNull @Positive Long fromStopId,
        @NotNull @Positive Long toStopId,
        @Min(1) @Max(9) Byte seats
) {
}
