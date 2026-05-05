package com.example.blablacar.service.user;

import com.example.blablacar.dto.user.UserRegistrationRequestDto;
import com.example.blablacar.dto.user.UserResponseDto;
import com.example.blablacar.exception.user.EmailAlreadyExistsException;
import com.example.blablacar.exception.user.InvalidAgeException;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
public class RegistrationService {

    private static final int MINIMUM_AGE = 18;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(final UserRepository userRepository,
                               final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDto register(final UserRegistrationRequestDto userRegistrationRequest) {
        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        validateAge(userRegistrationRequest.birthday());

        User user = new User(userRegistrationRequest.name(), userRegistrationRequest.email(),
                passwordEncoder.encode(userRegistrationRequest.password()),
                userRegistrationRequest.gender(),
                userRegistrationRequest.birthday(), userRegistrationRequest.phoneNumber());
        return UserResponseDto.from(userRepository.save(user));
    }

    public void validateAge(final LocalDate birthday) {
        if (birthday == null) {
            throw new InvalidAgeException("Birthday cannot be null");
        }

        int age = Period.between(birthday, LocalDate.now()).getYears();

        if (age < MINIMUM_AGE) {
            throw new InvalidAgeException("User must be at least 18 years old");
        }
    }
}
