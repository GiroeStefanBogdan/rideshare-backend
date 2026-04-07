package com.example.blablacar.dto.user.car;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 *
 */
public record UserCarResponse(
        long id,
        long userId,
        @JsonUnwrapped
        UserCarRequest details
) {
}
