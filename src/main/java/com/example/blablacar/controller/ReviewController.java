package com.example.blablacar.controller;

import com.example.blablacar.dto.review.MyReviewsDto;
import com.example.blablacar.dto.review.ReviewEligibilityDto;
import com.example.blablacar.dto.review.ReviewModerationRequestDto;
import com.example.blablacar.dto.review.ReviewRequestDto;
import com.example.blablacar.dto.review.ReviewResponseDto;
import com.example.blablacar.dto.review.UserReviewsDto;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.user.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(final ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponseDto> submitReview(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @Valid @RequestBody final ReviewRequestDto request) {
        return ResponseEntity.ok(reviewService.submit(userPrincipal.getUser(), request));
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<MyReviewsDto> getMyReviews(
            @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reviewService.getMyReviews(userPrincipal.getUser()));
    }

    @GetMapping("/reviews/me/eligibility")
    public ResponseEntity<List<ReviewEligibilityDto>> getMyReviewEligibility(
            @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reviewService.getEligibility(userPrincipal.getUser()));
    }

    @GetMapping("/users/{id}/reviews")
    public ResponseEntity<UserReviewsDto> getPublicReviews(@PathVariable final long id) {
        return ResponseEntity.ok(reviewService.getPublicReviews(id));
    }

    @PatchMapping("/admin/reviews/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponseDto> hideReview(
            @PathVariable final long id,
            @Valid @RequestBody(required = false) final ReviewModerationRequestDto request) {
        return ResponseEntity.ok(reviewService.hide(id, request == null ? null : request.reason()));
    }

    @PatchMapping("/admin/reviews/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponseDto> restoreReview(@PathVariable final long id) {
        return ResponseEntity.ok(reviewService.restore(id));
    }
}