package com.example.blablacar.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequestDto(
        @NotNull(message = "Target user is required")
        Long targetUserId,

        @Min(value = 1, message = "Score must be at least 1")
        @Max(value = 5, message = "Score must be at most 5")
        int score,

        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String details
) {
}