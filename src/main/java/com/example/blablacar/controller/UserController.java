package com.example.blablacar.controller;

import com.example.blablacar.controller.api.UserOperations;
import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.LoginResponse;
import com.example.blablacar.dto.UserProfileDto;
import com.example.blablacar.dto.UserRegistrationRequestDto;
import com.example.blablacar.dto.UserResponseDto;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Validated
@RestController
public class UserController implements UserOperations {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponseDto> register(UserRegistrationRequestDto registerRequest) {
        UserResponseDto userResponseDto = userService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);

    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest user) {
        LoginResponse loginResponse = userService.verify(user);

        if (loginResponse == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ResponseCookie cookie = ResponseCookie.from("token", loginResponse.token()).httpOnly(true).secure(false) // use true on HTTPS
                .path("/").sameSite("Lax").maxAge(Duration.ofHours(24)).build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);
    }


    @Override
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    @Override
    public ResponseEntity<UserProfileDto> getUserById(UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        UserProfileDto profile = userService.getUserById(authenticatedUser);
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<Void> changeUserPassword(UserPrincipal userPrincipal, LoginRequest loginRequest) {
        User user = userPrincipal.getUser();
        userService.changeUserPassword(user.getEmail(), loginRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserResponseDto> updateUserRole(long id, Role role) {
        UserResponseDto userResponseDto = userService.updateUserRole(id, role);
        return ResponseEntity.ok(userResponseDto);
    }

    @Override
    public ResponseEntity<Void> deleteUserById(Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMyAccount(UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        userService.deleteMyAccount(authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }


}
