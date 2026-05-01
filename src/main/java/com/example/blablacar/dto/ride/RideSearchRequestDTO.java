package com.example.blablacar.dto.ride;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Request body for {@code POST /rides/search}.
 * <p>
 * The frontend sends the location id together with its type
 * ({@code ADMIN_UNIT} vs {@code STREET}) so no ID-prefixing is needed.
 */
public record RideSearchRequestDTO(
        @NotNull @Positive Long fromId,
        @NotNull String fromType,
        @NotNull @Positive Long toId,
        @NotNull String toType,
        @NotNull LocalDate date,
        @Min(1) int seats,

        // Optional filters
        Double maxDistanceStart,
        Double maxDistanceEnd,
        Integer maxPrice,
        String timeWindow,       // BEFORE_8, 8_12, 12_18, AFTER_18
        Boolean smokingAllowed,
        Boolean petFriendly
) {
}
