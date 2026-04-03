package com.example.blablacar.controller;

import com.example.blablacar.controller.api.DashboardOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication-related endpoints
 */

@RestController
public class DashboardController implements DashboardOperations {

    @Override
    public ResponseEntity<String> getDashboard(UserDetails userDetails) {
        return ResponseEntity.ok(userDetails.getUsername());
    }

}
