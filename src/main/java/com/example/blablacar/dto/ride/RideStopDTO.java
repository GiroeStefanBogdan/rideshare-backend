package com.example.blablacar.dto.ride;

import com.example.blablacar.model.location.AdministrativeUnitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record RideStopDTO(@Positive Long id,
                          @NotNull AdministrativeUnitType type,
                          @Positive Byte stopOrder,
                          @Positive Short price) {
}
