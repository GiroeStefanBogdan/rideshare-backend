package com.example.blablacar.dto.review;

import jakarta.validation.constraints.Size;

public record ReviewModerationRequestDto(
        @Size(max = 200, message = "Reason must be at most 200 characters")
        String reason
) {
}