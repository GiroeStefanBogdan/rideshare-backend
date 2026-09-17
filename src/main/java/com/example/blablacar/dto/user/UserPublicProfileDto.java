package com.example.blablacar.dto.user;

import com.example.blablacar.dto.review.RatingSummaryDto;
import com.example.blablacar.dto.review.ReviewResponseDto;
import com.example.blablacar.model.enums.Gender;
import com.example.blablacar.model.user.User;

import java.time.LocalDate;
import java.util.List;

public record UserPublicProfileDto(
        long id,
        String name,
        LocalDate birthday,
        Gender gender,
        Double rating,
        Integer reviewsCount,
        List<ReviewResponseDto> reviews
) implements UserProfileDto {

    public static UserPublicProfileDto from(final User user) {
        return from(user, RatingSummaryDto.unrated(), List.of());
    }

    public static UserPublicProfileDto from(final User user, final RatingSummaryDto summary,
                                            final List<ReviewResponseDto> reviews) {
        return new UserPublicProfileDto(
                user.getId(),
                user.getName(),
                user.getBirthday(),
                user.getGender(),
                summary.average(),
                summary.count(),
                reviews
        );
    }
}
