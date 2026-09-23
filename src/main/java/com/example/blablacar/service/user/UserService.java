package com.example.blablacar.service.user;

import com.example.blablacar.dto.user.UpdateUserRequest;
import com.example.blablacar.dto.user.UpdateUserPreferencesRequest;
import com.example.blablacar.dto.user.UserProfileDto;
import com.example.blablacar.dto.user.UserPublicProfileDto;
import com.example.blablacar.dto.user.UserResponseDto;
import com.example.blablacar.dto.review.UserReviewsDto;
import com.example.blablacar.exception.user.EmailAlreadyExistsException;
import com.example.blablacar.exception.user.UserNotFoundException;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.UserInfo;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.user.UserInfoRepository;
import com.example.blablacar.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;
    private final ReviewService reviewService;

    public UserService(final UserRepository userRepository,
                       final UserInfoRepository userInfoRepository,
                       final PasswordEncoder passwordEncoder,
                       final RegistrationService registrationService,
                       final ReviewService reviewService) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
        this.reviewService = reviewService;
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .toList();
    }

    public UserProfileDto getProfile(final User authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
        return getFullProfile(user);
    }

    @Transactional
    public UserResponseDto updatePreferences(final User authenticatedUser,
                                             final UpdateUserPreferencesRequest updateRequest) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
        UserInfo userInfo = user.getUserInfo();
        if (userInfo == null) {
            // Should not happen after V9 migration + registration fix, but be defensive
            userInfo = new UserInfo(user);
            userInfo.setCanSmoke(false);
            userInfo.setPetFriendly(false);
            user.setUserInfo(userInfo);
        }
        if (updateRequest.canSmoke() != null) {
            userInfo.setCanSmoke(updateRequest.canSmoke());
        }
        if (updateRequest.petFriendly() != null) {
            userInfo.setPetFriendly(updateRequest.petFriendly());
        }
        userInfoRepository.save(userInfo);
        return UserResponseDto.from(user);
    }

    public UserPublicProfileDto getPublicProfile(final long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        UserReviewsDto reviews = reviewService.getPublicReviews(id);
        return UserPublicProfileDto.from(user, reviews.summary(), reviews.reviews());
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

    @Transactional
    public void deleteUserById(final Long id) {
        userRepository.findById(id).ifPresent(user -> {
            // Reviews outlive the account: contributions stay, attribution is anonymized, and the
            // deleted member's own reputation and received listing are removed.
            reviewService.anonymizeForDeletedUser(user);
            userRepository.delete(user);
        });
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

    public UserInfoRepository getUserInfoRepository() {
        return userInfoRepository;
    }

}
