package com.example.blablacar.dto.ride;

import com.example.blablacar.model.location.LocationType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Request body for {@code POST /rides/search}.
 * <p>
 * The frontend sends the location id together with its type
 * ({@code ADMIN_UNIT} vs {@code STREET}) so no ID-prefixing is needed.
 */
public record RideSearchRequestDTO(
        @NotNull @Positive Long fromId,
        @NotNull LocationType fromType,
        @NotNull @Positive Long toId,
        @NotNull LocationType toType,
        @NotNull @FutureOrPresent LocalDate date,
        @Min(1) int seats,

        // Optional filters
        @PositiveOrZero Double maxDistanceStart,
        @PositiveOrZero Double maxDistanceEnd,
        @PositiveOrZero Integer maxPrice,
        @Pattern(regexp = "BEFORE_8|8_12|12_18|AFTER_18") String timeWindow,
        Boolean smokingAllowed,
        Boolean petFriendly
) {
}
