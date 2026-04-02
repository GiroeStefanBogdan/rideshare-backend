package com.example.blablacar.dto;

import com.example.blablacar.model.enums.Gender;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;

import java.time.LocalDate;

public record UserResponseDto(
        long id,
        String name,
        String email,
        Role role,
        String phoneNumber,
        LocalDate birthday,
        Gender gender
) implements UserProfileDto {

    public static UserResponseDto from(User user){
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhoneNumber(),
                user.getBirthday(),
                user.getGender()
        );
    }
}
