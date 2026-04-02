package com.example.blablacar.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Interface defining dashboard-related operations for the v1 API.
 */
public interface DashboardOperations extends V1Api {

    @GetMapping("/dashboard")
    ResponseEntity<String> getDashboard(@AuthenticationPrincipal UserDetails userDetails);
}
