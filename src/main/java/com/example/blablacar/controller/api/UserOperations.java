package com.example.blablacar.controller.api;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.LoginResponse;
import com.example.blablacar.dto.UserProfileDto;
import com.example.blablacar.dto.UserRegistrationRequestDto;
import com.example.blablacar.dto.UserResponseDto;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Interface defining user-related operations for the v1 API.
 */
public interface UserOperations extends V1Api {

    @PostMapping("/register")
    ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationRequestDto registerRequest);

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest user);

    @GetMapping("/users/all")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<List<UserResponseDto>> getAllUsers();

    @GetMapping("/users/me")
    ResponseEntity<UserProfileDto> getUserById(@AuthenticationPrincipal UserPrincipal userPrincipal);

    @PatchMapping("/users/me/password")
    ResponseEntity<Void> changeUserPassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody LoginRequest loginRequest);

    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponseDto> updateUserRole(@PathVariable long id, @Valid @RequestBody Role role);

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUserById(@PathVariable Long id);

    @DeleteMapping("/users/me")
    ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal UserPrincipal userPrincipal);
}
