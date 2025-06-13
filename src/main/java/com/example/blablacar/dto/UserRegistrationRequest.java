package com.example.blablacar.dto;

import com.example.blablacar.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class UserRegistrationRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Birthday is required and must be in the past")
    @Past
    private LocalDate birthday;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+4|)?(07[0-8][0-9]|02[0-9]{2}|03[0-9]{2})(\\s|\\.|\\-)?[0-9]{3}(\\s|\\.|\\-)?[0-9]{3}$",
            message = "Invalid Romanian phone number"
    )
    private String phoneNumber;

    @NotNull(message = "gender is required")
    private Gender gender;
}
