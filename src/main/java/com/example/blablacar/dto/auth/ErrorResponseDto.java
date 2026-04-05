package com.example.blablacar.dto.auth;

import java.time.LocalDateTime;

public record ErrorResponseDto (
        LocalDateTime timestamp,
        int status,
        String error,
        String message
){}
