package com.example.blablacar.controller;

import com.example.blablacar.dto.UpdateUserCarRequest;
import com.example.blablacar.dto.UserCarRequest;
import com.example.blablacar.dto.UserCarResponse;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.UserCarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 */
@RestController
@RequestMapping("/users/me/cars")
public class UserCarController {

    private final UserCarService userCarService;

    public UserCarController(UserCarService userCarService) {
        this.userCarService = userCarService;
    }

    @PostMapping
    public ResponseEntity<UserCarResponse> createUserCar(
            @RequestBody @Valid UserCarRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        UserCarResponse userCar = userCarService.createUserCar(request, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userCar);
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<Void> deleteUserCar(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                              @PathVariable long carId) {
        User authenticatedUser = userPrincipal.getUser();
        userCarService.deleteUserCar(authenticatedUser.getId(), carId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserCarResponse>> getUserCarsByUserId(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        List<UserCarResponse> userCars = userCarService.getUserCarsByUserId(authenticatedUser.getId());
        return ResponseEntity.ok(userCars);
    }

    @PatchMapping("/{carId}")
    public ResponseEntity<UserCarResponse> updateUserCar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid UpdateUserCarRequest request,
            @PathVariable long carId) {
        User authenticatedUser = userPrincipal.getUser();
        UserCarResponse updatedUserCar = userCarService.updateUserCar(request, authenticatedUser.getId(), carId);
        return ResponseEntity.ok(updatedUserCar);
    }
}
