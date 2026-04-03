package com.example.blablacar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 *
 */
public record UpdateUserCarRequest(

        @Size(max = 50)
        String brand,

        @Size(max = 50)
        String model,

        @Size(max = 30)
        String color,

        @Min(value = 1900, message = "Year must be valid")
        @Max(value = 2100, message = "Year must be valid")
        Integer year,

        @Size(max = 20)
        String licensePlate,

        @Min(value = 2, message = "Must have at least 2 seats")
        @Max(value = 9, message = "Cannot exceed 9 seats")
        Integer numberOfSeats
) {
}
