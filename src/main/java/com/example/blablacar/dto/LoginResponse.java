package com.example.blablacar.dto;

public record LoginResponse(
        String token,
        UserResponseDto user
) {
}
