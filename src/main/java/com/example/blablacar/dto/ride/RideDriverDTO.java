package com.example.blablacar.dto.ride;

public record RideDriverDTO(
        long id,
        String name,
        String avatarUrl,
        Double rating,
        Integer reviewsCount,
        boolean smokingAllowed,
        boolean petFriendly
) {
}
