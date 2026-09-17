package com.example.blablacar.service.user;

import com.example.blablacar.dto.review.MyReviewsDto;
import com.example.blablacar.dto.review.RatingSummaryDto;
import com.example.blablacar.dto.review.ReviewEligibilityDto;
import com.example.blablacar.dto.review.ReviewRequestDto;
import com.example.blablacar.dto.review.ReviewResponseDto;
import com.example.blablacar.dto.review.UserReviewsDto;
import com.example.blablacar.exception.review.ReviewEditLockedException;
import com.example.blablacar.exception.review.ReviewNotEligibleException;
import com.example.blablacar.exception.review.ReviewNotFoundException;
import com.example.blablacar.exception.review.ReviewWindowClosedException;
import com.example.blablacar.exception.user.UserNotFoundException;
import com.example.blablacar.model.enums.ReviewAuthorRole;
import com.example.blablacar.model.enums.ReviewStatus;
import com.example.blablacar.model.ride.Booking;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.model.user.UserReview;
import com.example.blablacar.repository.ride.BookingRepository;
import com.example.blablacar.repository.user.ReviewRepository;
import com.example.blablacar.repository.user.UserInfoRepository;
import com.example.blablacar.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: AlexandruDicu
 * Since: 17.09.2026
 * Reviews and reputation. Eligibility is always derived from persisted bookings, never from the
 * client, and publication follows {@link ReviewPolicy} so that pending content stays private while
 * previously published content stays visible.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ReviewService(final ReviewRepository reviewRepository,
                         final UserRepository userRepository,
                         final UserInfoRepository userInfoRepository,
                         final BookingRepository bookingRepository,
                         final Clock clock) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /** A counterpart the acting user shared a ride with, and the role the acting user held. */
    private record Experience(long counterpartId, String counterpartName, ReviewAuthorRole role,
                              long rideId, OffsetDateTime dropoffAt) {
    }

    @Transactional
    public ReviewResponseDto submit(final User actor, final ReviewRequestDto request) {
        if (request.targetUserId() == actor.getId()) {
            throw new ReviewNotEligibleException("You cannot review yourself");
        }

        User target = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new UserNotFoundException(request.targetUserId()));
        OffsetDateTime now = OffsetDateTime.now(clock);
        Experience experience = latestExperienceWith(actor.getId(), target.getId(), now);
        if (experience == null) {
            throw new ReviewNotEligibleException("You have no completed ride with this member");
        }

        UserReview review = reviewRepository
                .findByReviewerIdAndTargetUserId(actor.getId(), target.getId())
                .orElse(null);
        if (review == null) {
            if (!ReviewWindowPolicy.canReviewAfter(experience.dropoffAt(), now)) {
                throw new ReviewWindowClosedException("The review window for this ride has closed");
            }
            review = new UserReview(actor, target, request.score(), request.details(), LocalDate.now(clock),
                    experience.role());
        } else {
            // A strictly newer shared ride reopens the cycle (per ADR 0004) before the guard runs,
            // so a direct submit after a later ride works without a prior eligibility read.
            refreshCycle(review, experience, now);
            requireSubmittable(review, now);
        }

        review.setScore(request.score());
        review.setDetails(request.details());
        review.setReviewerRole(experience.role());
        review.setLatestSharedRideId(experience.rideId());
        review.setLatestSharedDropoffAt(experience.dropoffAt());
        review.setWindowEndsAt(ReviewWindowPolicy.windowEndsAt(experience.dropoffAt()));
        review.setSubmittedAt(now);
        review.setStatus(ReviewStatus.PENDING);
        review.setUpdatedAt(now);
        reviewRepository.save(review);

        publishReciprocally(review, actor.getId(), target.getId(), now);
        return toDto(review, true, now);
    }

    private void requireSubmittable(final UserReview review, final OffsetDateTime now) {
        if (review.getHiddenAt() != null) {
            throw new ReviewEditLockedException("This review was hidden by moderation");
        }
        if (!ReviewPolicy.isWindowOpen(review, now)) {
            throw new ReviewWindowClosedException("The review window for this ride has closed");
        }
        if (review.getStatus() == ReviewStatus.PUBLISHED) {
            throw new ReviewEditLockedException("This review is published and can only be changed after a later ride");
        }
    }

    /** Publishes due received reviews before computing reputation, independently of optional user info. */
    @Transactional
    public RatingSummaryDto getRatingSummary(final long userId) {
        List<UserReview> received = receivedReviews(userId, OffsetDateTime.now(clock));
        return summaryFor(received);
    }

    private List<UserReview> receivedReviews(final long userId, final OffsetDateTime now) {
        List<UserReview> received = reviewRepository.findAllReceivedByTargetUserId(userId);
        received.forEach(review -> publishDueIfEligible(review, now));
        return received;
    }

    @Transactional
    public UserReviewsDto getPublicReviews(final long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ReviewResponseDto> published = new ArrayList<>();
        List<UserReview> received = receivedReviews(userId, now);
        for (UserReview review : received) {
            if (ReviewPolicy.isVisibleToPublic(review)) {
                published.add(toDto(review, false, now));
            }
        }

        return new UserReviewsDto(summaryFor(received), published);
    }

    @Transactional
    public MyReviewsDto getMyReviews(final User actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Map<Long, Experience> experiences = eligibleExperiences(actor.getId(), now);
        List<ReviewEligibilityDto> toWrite = new ArrayList<>();

        for (Map.Entry<Long, Experience> entry : experiences.entrySet()) {
            UserReview review = reviewRepository
                    .findByReviewerIdAndTargetUserId(actor.getId(), entry.getKey())
                    .orElse(null);
            if (review != null && refreshCycle(review, entry.getValue(), now)) {
                reviewRepository.save(review);
            }
            toWrite.add(eligibility(entry.getValue(), review, now));
        }

        List<ReviewResponseDto> received = new ArrayList<>();
        for (UserReview review : reviewRepository.findAllReceivedByTargetUserId(actor.getId())) {
            publishDueIfEligible(review, now);
            if (ReviewPolicy.isVisibleToPublic(review)) {
                received.add(toDto(review, false, now));
            }
        }

        List<ReviewResponseDto> authored = reviewRepository
                .findAllByReviewerIdOrderByUpdatedAtDesc(actor.getId()).stream()
                .map(review -> {
                    publishDueIfEligible(review, now);
                    return toDto(review, true, now);
                })
                .toList();

        return new MyReviewsDto(summaryFor(actor.getId()), received, authored,
                toWrite.stream().filter(ReviewEligibilityDto::canSubmit).toList());
    }

    @Transactional
    public List<ReviewEligibilityDto> getEligibility(final User actor) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ReviewEligibilityDto> eligible = new ArrayList<>();
        for (Experience experience : eligibleExperiences(actor.getId(), now).values()) {
            UserReview review = reviewRepository
                    .findByReviewerIdAndTargetUserId(actor.getId(), experience.counterpartId())
                    .orElse(null);
            if (review != null && refreshCycle(review, experience, now)) {
                reviewRepository.save(review);
            }
            eligible.add(eligibility(experience, review, now));
        }

        return eligible;
    }

    @Transactional
    public ReviewResponseDto hide(final long reviewId, final String reason) {
        UserReview review = requireReview(reviewId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        review.setHiddenAt(now);
        review.setHiddenReason(reason);
        review.setStatus(ReviewStatus.HIDDEN);
        review.setUpdatedAt(now);
        reviewRepository.save(review);
        recomputeRating(review.getTargetUser().getId());

        return toDto(review, false, now);
    }

    @Transactional
    public ReviewResponseDto restore(final long reviewId) {
        UserReview review = requireReview(reviewId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        review.setHiddenAt(null);
        review.setHiddenReason(null);
        review.setStatus(review.getPublishedAt() == null ? ReviewStatus.PENDING : ReviewStatus.PUBLISHED);
        review.setUpdatedAt(now);
        reviewRepository.save(review);
        recomputeRating(review.getTargetUser().getId());

        return toDto(review, false, now);
    }

    private UserReview requireReview(final long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("No review with id " + reviewId));
    }

    @Transactional
    public void anonymizeForDeletedUser(final User user) {
        long userId = user.getId();
        List<UserReview> received = reviewRepository.findAllReceivedByTargetUserId(userId);
        if (!received.isEmpty()) {
            // A deleted member has no reputation left to display, and nothing may reference them.
            reviewRepository.deleteAll(received);
        }

        for (UserReview review : reviewRepository.findAllByReviewerIdOrderByUpdatedAtDesc(userId)) {
            review.clearReviewer();
            review.setReviewerName(ReviewPolicy.DELETED_MEMBER);
            reviewRepository.save(review);
            if (review.getTargetUser() != null) {
                recomputeRating(review.getTargetUser().getId());
            }
        }
    }

    private void publishReciprocally(final UserReview review, final long actorId, final long targetId,
                                     final OffsetDateTime now) {
        UserReview counterpart = reviewRepository
                .findByReviewerIdAndTargetUserId(targetId, actorId)
                .orElse(null);

        // Both decisions are taken before either review is published, because publishing one clears
        // the pending state the other's mutual-publication check depends on.
        boolean publishMine = ReviewPolicy.shouldPublish(review, counterpart, now);
        boolean publishTheirs = ReviewPolicy.shouldPublish(counterpart, review, now);

        if (publishMine) {
            publish(review, now);
            reviewRepository.save(review);
            recomputeRating(targetId);
        }
        if (publishTheirs) {
            publish(counterpart, now);
            reviewRepository.save(counterpart);
            recomputeRating(actorId);
        }
    }

    private boolean publishDueIfEligible(final UserReview review, final OffsetDateTime now) {
        if (review.getTargetUser() == null || !ReviewPolicy.isPendingSubmission(review)) {
            return false;
        }

        UserReview counterpart = review.getReviewer() == null ? null : reviewRepository
                .findByReviewerIdAndTargetUserId(review.getTargetUser().getId(), review.getReviewer().getId())
                .orElse(null);
        if (!ReviewPolicy.shouldPublish(review, counterpart, now)) {
            return false;
        }

        publish(review, now);
        reviewRepository.save(review);
        recomputeRating(review.getTargetUser().getId());
        return true;
    }

    private void publish(final UserReview review, final OffsetDateTime now) {
        review.setPublishedScore(review.getScore());
        review.setPublishedDetails(review.getDetails());
        review.setPublishedRole(review.getReviewerRole());
        review.setPublishedAt(now);
        review.setStatus(ReviewStatus.PUBLISHED);
        review.setUpdatedAt(now);
    }

    /** Reopens the review for a later shared ride, keeping the previously published content live. */
    private boolean refreshCycle(final UserReview review, final Experience experience, final OffsetDateTime now) {
        if (!ReviewPolicy.opensNewCycle(review, experience.dropoffAt(), now)) {
            return false;
        }

        // Settle the old submission before clearing its cycle state.
        publishDueIfEligible(review, now);
        review.setLatestSharedRideId(experience.rideId());
        review.setLatestSharedDropoffAt(experience.dropoffAt());
        review.setWindowEndsAt(ReviewWindowPolicy.windowEndsAt(experience.dropoffAt()));
        review.setReviewerRole(experience.role());
        review.setSubmittedAt(null);
        review.setStatus(ReviewStatus.PENDING);
        review.setUpdatedAt(now);
        return true;
    }

    private ReviewEligibilityDto eligibility(final Experience experience, final UserReview review,
                                             final OffsetDateTime now) {
        boolean canSubmit = review == null
                ? ReviewWindowPolicy.canReviewAfter(experience.dropoffAt(), now)
                : ReviewPolicy.canSubmit(review, now);
        OffsetDateTime windowEndsAt = review == null || review.getWindowEndsAt() == null
                ? ReviewWindowPolicy.windowEndsAt(experience.dropoffAt())
                : review.getWindowEndsAt();

        return new ReviewEligibilityDto(experience.counterpartId(), experience.counterpartName(),
                experience.role(), experience.rideId(), experience.dropoffAt(), windowEndsAt,
                review == null ? null : review.getId(), canSubmit);
    }

    private Map<Long, Experience> eligibleExperiences(final long userId, final OffsetDateTime now) {
        OffsetDateTime from = now.minusDays(14);
        Map<Long, Experience> byCounterpart = new LinkedHashMap<>();

        for (Booking booking : bookingRepository.findReviewableBookingsAsPassenger(userId, from, now)) {
            addExperience(byCounterpart, booking.getRide().getDriver(), ReviewAuthorRole.PASSENGER,
                    booking.getRide().getId(), booking.getToStop().getDepartsAt());
        }
        for (Booking booking : bookingRepository.findReviewableBookingsAsDriver(userId, from, now)) {
            addExperience(byCounterpart, booking.getPassenger(), ReviewAuthorRole.DRIVER,
                    booking.getRide().getId(), booking.getToStop().getDepartsAt());
        }
        // Drivers may currently book their own rides as a testing convenience; nobody reviews themselves.
        byCounterpart.remove(userId);

        return byCounterpart;
    }

    private Experience latestExperienceWith(final long actorId, final long targetId, final OffsetDateTime now) {
        return eligibleExperiences(actorId, now).get(targetId);
    }

    private void addExperience(final Map<Long, Experience> byCounterpart, final User counterpart,
                               final ReviewAuthorRole role, final Long rideId, final OffsetDateTime dropoffAt) {
        if (counterpart == null || dropoffAt == null) {
            return;
        }

        Experience existing = byCounterpart.get(counterpart.getId());
        if (existing == null || dropoffAt.isAfter(existing.dropoffAt())) {
            byCounterpart.put(counterpart.getId(), new Experience(counterpart.getId(), counterpart.getName(),
                    role, rideId, dropoffAt));
        }
    }

    private ReviewResponseDto toDto(final UserReview review, final boolean forAuthor, final OffsetDateTime now) {
        boolean pending = ReviewPolicy.isPendingSubmission(review);
        boolean edited = review.getPublishedAt() != null && review.getUpdatedAt() != null
                && review.getUpdatedAt().isAfter(review.getPublishedAt());
        ReviewAuthorRole role = review.getPublishedRole() != null
                ? review.getPublishedRole()
                : review.getReviewerRole();

        return new ReviewResponseDto(
                review.getId(),
                review.getReviewer() == null ? null : review.getReviewer().getId(),
                review.getReviewerName(),
                review.getTargetUser() == null ? null : review.getTargetUser().getId(),
                review.getPublishedScore(),
                review.getPublishedDetails(),
                role,
                review.getPublishedAt(),
                edited,
                pending,
                ReviewPolicy.isVisibleToPublic(review),
                forAuthor && pending ? review.getScore() : null,
                forAuthor && pending ? review.getDetails() : null,
                review.getWindowEndsAt(),
                forAuthor && ReviewPolicy.canSubmit(review, now),
                review.getStatus());
    }

    private RatingSummaryDto summaryFor(final long userId) {
        return summaryFor(reviewRepository.findAllReceivedByTargetUserId(userId));
    }

    private RatingSummaryDto summaryFor(final List<UserReview> received) {
        int count = 0;
        int sum = 0;
        for (UserReview review : received) {
            Integer score = ReviewPolicy.visibleScore(review);
            if (score != null) {
                count++;
                sum += score;
            }
        }
        if (count == 0) {
            return RatingSummaryDto.unrated();
        }

        return new RatingSummaryDto(Math.round(sum * 10.0 / count) / 10.0, count);
    }

    private void recomputeRating(final long userId) {
        UserInfo info = userInfoRepository.findById(userId).orElse(null);
        if (info == null) {
            return;
        }

        RatingSummaryDto summary = summaryFor(userId);
        info.setRating(summary.average());
        info.setReviewsCount(summary.count());
        userInfoRepository.save(info);
    }
}