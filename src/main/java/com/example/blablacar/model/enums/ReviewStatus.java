package com.example.blablacar.model.enums;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Lifecycle state of a review's current cycle. {@code PENDING} content is visible only to its
 * author, {@code PUBLISHED} content is public and counts toward reputation, and {@code HIDDEN}
 * content was removed from public sight by moderation.
 */
public enum ReviewStatus {
    PENDING, PUBLISHED, HIDDEN
}