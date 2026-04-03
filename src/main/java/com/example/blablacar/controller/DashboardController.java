package com.example.blablacar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication-related endpoints
 */

@RestController
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(userDetails.getUsername());
    }

}
