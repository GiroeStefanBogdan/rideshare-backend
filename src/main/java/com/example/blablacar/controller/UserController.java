package com.example.blablacar.controller;

import com.example.blablacar.dto.*;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@Validated
@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationRequestDto registerRequest) {
        UserResponseDto userResponseDto = userService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest user) {
        String token = userService.verify(user);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        ResponseCookie cookie = ResponseCookie.from("token", token).httpOnly(true).secure(false) // use true on HTTPS
                .path("/").sameSite("Lax").maxAge(Duration.ofHours(24)).build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }


    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable long id,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        Object profile = userService.getUserById(id, authenticatedUser);
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changeUserPassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody LoginRequest loginRequest) {
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

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteMyAccount(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // only allow deleting own account
        User authenticatedUser = userPrincipal.getUser();
        if (authenticatedUser.getId() != id && authenticatedUser.getRole().equals(Role.ROLE_ADMIN)) {
            return ResponseEntity.noContent().build();
        }
        userService.deleteMyAccount(id);
        return ResponseEntity.noContent().build();
    }


}
