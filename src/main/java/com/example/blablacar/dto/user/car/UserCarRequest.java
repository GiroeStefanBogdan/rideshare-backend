package com.example.blablacar.dto.user.car;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 *
 */
public record UserCarRequest(

        @NotBlank(message = "Brand is required")
        @Size(max = 50)
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 50)
        String model,

        @NotBlank(message = "Color is required")
        @Size(max = 30)
        String color,

        @Min(value = 1900, message = "Year must be valid")
        @Max(value = 2100, message = "Year must be valid")
        Integer year,

        @NotBlank(message = "License plate is required")
        @Size(max = 20)
        String licensePlate,

        @Min(value = 2, message = "Must have at least 2 seats")
        @Max(value = 9, message = "Cannot exceed 9 seats")
        Integer numberOfSeats
) {
}
