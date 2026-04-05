package com.example.blablacar.controller;

import com.example.blablacar.dto.*;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.UserService;
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

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationRequestDto registerRequest) {
        UserResponseDto userResponseDto = userService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);

    }

    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest user) {
        LoginResponse loginResponse = userService.verify(user);

        if (loginResponse == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ResponseCookie cookie = ResponseCookie.from("token", loginResponse.token()).httpOnly(true).secure(false) // use true on HTTPS
                .path("/").sameSite("Lax").maxAge(Duration.ofHours(24)).build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileDto> getUserById(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        UserProfileDto profile = userService.getUserById(authenticatedUser);
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changeUserPassword(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                   LoginRequest loginRequest) {
        User user = userPrincipal.getUser();
        userService.changeUserPassword(user.getEmail(), loginRequest);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserRole(@PathVariable long id, @Valid @RequestBody Role role) {
        UserResponseDto userResponseDto = userService.updateUserRole(id, role);
        return ResponseEntity.ok(userResponseDto);
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteMyAccount(userPrincipal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("users/me")
    public ResponseEntity<UserResponseDto> updateUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                             @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        User authenticatedUser = userPrincipal.getUser();
        UserResponseDto userResponseDto = userService.updateUserProfile(authenticatedUser, updateUserRequest);

        return ResponseEntity.ok(userResponseDto);
    }
}
