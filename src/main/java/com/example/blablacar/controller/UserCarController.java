package com.example.blablacar.controller;

import com.example.blablacar.controller.api.UserCarOperations;
import com.example.blablacar.dto.UpdateUserCarRequest;
import com.example.blablacar.dto.UserCarRequest;
import com.example.blablacar.dto.UserCarResponse;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.UserCarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 */
@RestController
public class UserCarController implements UserCarOperations {

    private final UserCarService userCarService;

    public UserCarController(UserCarService userCarService) {
        this.userCarService = userCarService;
    }

    @Override
    public ResponseEntity<UserCarResponse> createUserCar(
            UserCarRequest request,
            UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        UserCarResponse userCar = userCarService.createUserCar(request, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userCar);
    }

    @Override
    public ResponseEntity<Void> deleteUserCar(UserPrincipal userPrincipal, long carId) {
        User authenticatedUser = userPrincipal.getUser();
        userCarService.deleteUserCar(authenticatedUser.getId(), carId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<UserCarResponse>> getUserCarsByUserId(
            UserPrincipal userPrincipal) {
        User authenticatedUser = userPrincipal.getUser();
        List<UserCarResponse> userCars = userCarService.getUserCarsByUserId(authenticatedUser.getId());
        return ResponseEntity.ok(userCars);
    }

    @Override
    public ResponseEntity<UserCarResponse> updateUserCar(
            UserPrincipal userPrincipal,
            UpdateUserCarRequest request,
            long carId) {
        User authenticatedUser = userPrincipal.getUser();
        UserCarResponse updatedUserCar = userCarService.updateUserCar(request, authenticatedUser.getId(), carId);
        return ResponseEntity.ok(updatedUserCar);
    }
}
