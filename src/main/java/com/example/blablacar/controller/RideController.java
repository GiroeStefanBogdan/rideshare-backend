package com.example.blablacar.controller;

import com.example.blablacar.dto.ride.*;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.ride.RideService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<Optional<Long>> saveNewRide(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @RequestBody @Valid final RideDTO rideRequest) {
        long rideId = rideService.save(userPrincipal.getUser(), rideRequest);
        return ResponseEntity.created(URI.create("/rides/" + rideId)).body(Optional.of(rideId));
    }

    @GetMapping("/me")
    public ResponseEntity<MyRidesResponseDTO> getMyRides(
            @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        return ResponseEntity.ok(rideService.getMyRides(userPrincipal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideDetailsDTO> getRideDetails(@PathVariable final long id) {
        return ResponseEntity.ok(rideService.getRideDetails(id));
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
                                           @PathVariable long id) {
        rideService.deleteRide(userPrincipal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public List<RideSearchResultDTO> search(@RequestBody @Valid RideSearchRequestDTO request) {
        return rideService.searchRides(request);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Optional<Long>> reserveSeats(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable long id,
            @RequestBody @Valid ReserveRideRequestDTO request) {
        long bookingId = rideService.reserveSeats(userPrincipal.getUser(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Optional.of(bookingId));
    }
}
