package com.example.blablacar.model.user;


import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Author: AlexandruDicu
 * Since: 24.06.2025
 * One directional review between two users who shared a ride. A user keeps at most one review per
 * counterpart for life; a later shared ride reopens its window instead of creating a second review.
 * The {@code reviewer}/{@code targetUser} links may be cleared when an account is deleted, leaving
 * the review attributed to {@code reviewerName} ("Deleted member") or removed from public sight.
 */
@Entity
@Table(name = "user_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uc_user_review_pair", columnNames = {"reviewer_id", "target_user_id"})
})
public class UserReview implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_name", length = 64)
    private String reviewerName;

    @Column(nullable = false)
    private int score;

    @Column(length = 1000)
    private String details;

    @Column(name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_role", length = 20)
    private ReviewAuthorRole reviewerRole;

    @Column(name = "latest_shared_ride_id")
    private Long latestSharedRideId;

    @Column(name = "latest_shared_dropoff_at")
    private OffsetDateTime latestSharedDropoffAt;

    @Column(name = "window_ends_at")
    private OffsetDateTime windowEndsAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_score")
    private Integer publishedScore;

    @Column(name = "published_details", length = 1000)
    private String publishedDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "published_role", length = 20)
    private ReviewAuthorRole publishedRole;

    @Column(name = "hidden_at")
    private OffsetDateTime hiddenAt;

    @Column(name = "hidden_reason", length = 200)
    private String hiddenReason;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected UserReview() {
    }

    public UserReview(final User reviewer, final User targetUser, final int score, final String details,
                      final LocalDate date, final ReviewAuthorRole reviewerRole) {
        this.reviewer = reviewer;
        this.targetUser = targetUser;
        this.reviewerName = reviewer.getName();
        this.score = score;
        this.details = details;
        this.date = date;
        this.reviewerRole = reviewerRole;
        this.status = ReviewStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public void clearTargetUser() {
        this.targetUser = null;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void clearReviewer() {
        this.reviewer = null;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(final String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(final int score) {
        this.score = score;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(final ReviewStatus status) {
        this.status = status;
    }

    public ReviewAuthorRole getReviewerRole() {
        return reviewerRole;
    }

    public void setReviewerRole(final ReviewAuthorRole reviewerRole) {
        this.reviewerRole = reviewerRole;
    }

    public Long getLatestSharedRideId() {
        return latestSharedRideId;
    }

    public void setLatestSharedRideId(final Long latestSharedRideId) {
        this.latestSharedRideId = latestSharedRideId;
    }

    public OffsetDateTime getLatestSharedDropoffAt() {
        return latestSharedDropoffAt;
    }

    public void setLatestSharedDropoffAt(final OffsetDateTime latestSharedDropoffAt) {
        this.latestSharedDropoffAt = latestSharedDropoffAt;
    }

    public OffsetDateTime getWindowEndsAt() {
        return windowEndsAt;
    }

    public void setWindowEndsAt(final OffsetDateTime windowEndsAt) {
        this.windowEndsAt = windowEndsAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(final OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(final OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getPublishedScore() {
        return publishedScore;
    }

    public void setPublishedScore(final Integer publishedScore) {
        this.publishedScore = publishedScore;
    }

    public String getPublishedDetails() {
        return publishedDetails;
    }

    public void setPublishedDetails(final String publishedDetails) {
        this.publishedDetails = publishedDetails;
    }

    public ReviewAuthorRole getPublishedRole() {
        return publishedRole;
    }

    public void setPublishedRole(final ReviewAuthorRole publishedRole) {
        this.publishedRole = publishedRole;
    }

    public OffsetDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(final OffsetDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    public String getHiddenReason() {
        return hiddenReason;
    }

    public void setHiddenReason(final String hiddenReason) {
        this.hiddenReason = hiddenReason;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}