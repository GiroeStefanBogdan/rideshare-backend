package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record RideStopDTO(@Positive Long id,
                          @NotNull String type,
                          @Positive Byte stopOrder,
                          @Positive Short price) {
}
