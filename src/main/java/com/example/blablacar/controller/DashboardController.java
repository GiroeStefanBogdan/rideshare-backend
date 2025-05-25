package com.example.blablacar.controller;

import com.example.blablacar.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication-related endpoints
 */

@RequestMapping("/api")
@RestController
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<UserResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(new UserResponse(userDetails.getUsername()));
    }

}
