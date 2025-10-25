package com.example.blablacar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication-related endpoints
 */

@RestController
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails.getUsername());
    }

}
