package com.example.blablacar.dto.user;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 * Sealed interface representing a user profile response.
 * Either a full profile ({@link UserResponseDto}) or a public profile ({@link UserPublicProfileDto}).
 */
public sealed interface UserProfileDto permits UserResponseDto, UserPublicProfileDto {
}
