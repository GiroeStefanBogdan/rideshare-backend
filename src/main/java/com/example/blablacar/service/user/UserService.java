package com.example.blablacar.service.user;

import com.example.blablacar.dto.user.UpdateUserRequest;
import com.example.blablacar.dto.user.UserProfileDto;
import com.example.blablacar.dto.user.UserPublicProfileDto;
import com.example.blablacar.dto.user.UserResponseDto;
import com.example.blablacar.exception.user.EmailAlreadyExistsException;
import com.example.blablacar.exception.user.UserNotFoundException;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;

    public UserService(final UserRepository userRepository,
                       final PasswordEncoder passwordEncoder,
                       final RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .toList();
    }

    public UserProfileDto getProfile(final User authenticatedUser) {
        return getFullProfile(authenticatedUser);
    }

    public UserPublicProfileDto getPublicProfile(final long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return UserPublicProfileDto.from(user);
    }

    public UserResponseDto updateProfile(final User authenticatedUser,
                                         final UpdateUserRequest updateUserRequest) {

        if (updateUserRequest.email() != null && !updateUserRequest.email()
                .equalsIgnoreCase(authenticatedUser.getEmail())) {
            if (userRepository.existsByEmail(updateUserRequest.email())) {
                throw new EmailAlreadyExistsException("This email already exist");
            }

            authenticatedUser.setEmail(updateUserRequest.email());
        }

        if (updateUserRequest.name() != null && !updateUserRequest.name().isBlank()) {
            authenticatedUser.setName(updateUserRequest.name());
        }

        if (updateUserRequest.phoneNumber() != null && !updateUserRequest.phoneNumber().isBlank()) {
            authenticatedUser.setPhoneNumber(updateUserRequest.phoneNumber());
        }

        if (updateUserRequest.birthday() != null) {
            registrationService.validateAge(updateUserRequest.birthday());
            authenticatedUser.setBirthday(updateUserRequest.birthday());
        }

        if (updateUserRequest.gender() != null) {
            authenticatedUser.setGender(updateUserRequest.gender());
        }

        return UserResponseDto.from(userRepository.save(authenticatedUser));
    }

    public void changePassword(final String email, final String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email " + email));
        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }

    public UserResponseDto updateUserRole(final long id, final Role newRole) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setRole(newRole);

        return UserResponseDto.from(userRepository.save(user));
    }

    public void deleteUserById(final Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
    }

    public void deleteMyAccount(final Long id) {
        deleteUserById(id);
    }

    public User getReferenceById(final long id) {
        return userRepository.getReferenceById(id);
    }

    private UserResponseDto getFullProfile(final User user) {
        return UserResponseDto.from(user);
    }

}
