package com.example.blablacar.dto.user;

import com.example.blablacar.model.enums.Gender;
import com.example.blablacar.model.user.User;

import java.time.LocalDate;

public record UserPublicProfileDto(
        long id,
        String name,
        LocalDate birthday,
        Gender gender
) implements UserProfileDto {

    public static UserPublicProfileDto from(User user){
        return new UserPublicProfileDto(
                user.getId(),
                user.getName(),
                user.getBirthday(),
                user.getGender()
        );
    }
}