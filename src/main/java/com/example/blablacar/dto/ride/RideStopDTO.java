package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record RideStopDTO(@Positive Long id,
                          @NotNull String type,
                          @Positive Byte stopOrder,
                          @PositiveOrZero Short price,
                          @NotNull OffsetDateTime departsAt) {
}
