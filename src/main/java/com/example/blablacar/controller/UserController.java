package com.example.blablacar.controller;

import com.example.blablacar.dto.auth.LoginRequest;
import com.example.blablacar.dto.auth.LoginResponse;
import com.example.blablacar.dto.user.UpdateUserRequest;
import com.example.blablacar.dto.user.UserProfileDto;
import com.example.blablacar.dto.user.UserPublicProfileDto;
import com.example.blablacar.dto.user.UserRegistrationRequestDto;
import com.example.blablacar.dto.user.UserResponseDto;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.auth.AuthenticationService;
import com.example.blablacar.service.user.RegistrationService;
import com.example.blablacar.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Validated
@RestController
public class UserController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    @Autowired
    public UserController(final RegistrationService registrationService,
                          final AuthenticationService authenticationService,
                          final UserService userService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody final UserRegistrationRequestDto registerRequest) {
        UserResponseDto userResponseDto = registrationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid final LoginRequest user) {
        LoginResponse loginResponse = authenticationService.verify(user);
        ResponseCookie cookie = ResponseCookie.from("token", loginResponse.token())
                .httpOnly(true).secure(false)
                .path("/").sameSite("Lax").maxAge(Duration.ofHours(24)).build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true).secure(false)
                .path("/").sameSite("Lax").maxAge(Duration.ZERO).build();

        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileDto> getUserById(
            @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        UserProfileDto profile = userService.getProfile(authenticatedUser);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserPublicProfileDto> getPublicUserProfile(@PathVariable final long id) {
        return ResponseEntity.ok(userService.getPublicProfile(id));
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changeUserPassword(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @RequestBody final LoginRequest loginRequest) {
        User user = userPrincipal.getUser();
        userService.changePassword(user.getEmail(), loginRequest.getPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserRole(@PathVariable final long id,
                                                          @Valid @RequestBody final Role role) {
        UserResponseDto userResponseDto = userService.updateUserRole(id, role);
        return ResponseEntity.ok(userResponseDto);
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable final Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        userService.deleteMyAccount(userPrincipal.getUser().getId());
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true).secure(false)
                .path("/").sameSite("Lax").maxAge(Duration.ZERO).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PatchMapping("users/me")
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @Valid @RequestBody final UpdateUserRequest updateUserRequest) {
        User authenticatedUser = userPrincipal.getUser();
        UserResponseDto userResponseDto = userService.updateProfile(authenticatedUser, updateUserRequest);

        return ResponseEntity.ok(userResponseDto);
    }
}
