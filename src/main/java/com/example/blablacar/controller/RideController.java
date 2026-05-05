package com.example.blablacar.controller;

import com.example.blablacar.dto.ride.ReserveRideRequestDTO;
import com.example.blablacar.dto.ride.RideDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.ride.RideService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@RestController
@RequestMapping("/rides")
@Validated
public class RideController {

    private final RideService rideService;

    @Autowired
    public RideController(final RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<Long> saveNewRide(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                            @RequestBody @Valid RideDTO rideRequest) {
        return ResponseEntity.ok(rideService.save(userPrincipal.getUser(), rideRequest));
    }

    @PatchMapping("{id}/seats")
    public ResponseEntity<Void> updateSeatNumber(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                 @PathVariable long id,
                                 @RequestBody byte seatNumber) {
        rideService.updateSeatNumber(userPrincipal.getUser(), id, seatNumber);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                           @PathVariable long id){
        rideService.deleteRide(userPrincipal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public List<RideSearchResultDTO> search(@RequestBody @Valid RideSearchRequestDTO request) {
        return rideService.searchRides(request);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Long> reserveSeats(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable long id,
            @RequestBody @Valid ReserveRideRequestDTO request) {
        return ResponseEntity.ok(rideService.reserveSeats(userPrincipal.getUser(), id, request));
    }
}
