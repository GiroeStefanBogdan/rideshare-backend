package com.example.blablacar.dto.auth;

import com.example.blablacar.dto.user.UserResponseDto;

public record LoginResponse(
        String token,
        UserResponseDto user
) {
}
