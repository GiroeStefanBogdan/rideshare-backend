package com.example.blablacar.dto.location;

import java.math.BigDecimal;

/**
 * DTO returned by GET /locations/search.
 * {@code type} is "ADMIN_UNIT" or "STREET" so the frontend can
 * send it back in a ride-search request without any ID-prefixing.
 */
public record LocationResultDTO(
        long id,
        String type,
        String name,
        String fullName,
        BigDecimal latitude,
        BigDecimal longitude,
        Long population
) {
}
