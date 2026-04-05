package com.example.blablacar.dto.location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
public record AdministrativeUnitDTO(
        @Positive
        Long id,
        @NotNull
        String fullName
) {
}
