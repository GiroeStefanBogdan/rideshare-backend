package com.example.blablacar.dto.user;

/**
 * Request body for PATCH /users/me/preferences.
 * Both fields are optional — only non-null values are applied.
 */
public record UpdateUserPreferencesRequest(
        Boolean canSmoke,
        Boolean petFriendly
) {
}
