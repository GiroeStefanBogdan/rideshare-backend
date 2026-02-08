package com.example.blablacar.dto;

import com.example.blablacar.model.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserRequestDto(

        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Email(message = "Email should be valid")
        String email,

        @Pattern(regexp = "^(\\+4|)?(07[0-8][0-9]|02[0-9]{2}|03[0-9]{2})(\\s|\\.|-)?[0-9]{3}(\\s|\\.|-)?[0-9]{3}$", message = "Invalid Romanian phone number")
        String phoneNumber,

        @Past(message = "Birthday must be in the past")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthday,

        Gender gender

) {
}
