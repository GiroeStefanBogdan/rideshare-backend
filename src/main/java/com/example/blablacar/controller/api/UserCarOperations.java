package com.example.blablacar.controller.api;

import com.example.blablacar.dto.UserCarRequest;
import com.example.blablacar.dto.UserCarResponse;
import com.example.blablacar.dto.UpdateUserCarRequest;
import com.example.blablacar.model.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Interface defining car-related operations for the v1 API.
 */
@RequestMapping("/users/me/cars")
public interface UserCarOperations extends V1Api {

    @PostMapping
    ResponseEntity<UserCarResponse> createUserCar(
            @RequestBody @Valid UserCarRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal);

    @DeleteMapping("/{carId}")
    ResponseEntity<Void> deleteUserCar(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable long carId);

    @GetMapping
    ResponseEntity<List<UserCarResponse>> getUserCarsByUserId(
            @AuthenticationPrincipal UserPrincipal userPrincipal);

    @PatchMapping("/{carId}")
    ResponseEntity<UserCarResponse> updateUserCar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid UpdateUserCarRequest request,
            @PathVariable long carId);
}
