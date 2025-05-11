package com.example.blablacar.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication-related endpoints
 */
@RestController
public class DashboardController {


    @GetMapping("/greeting")
    public Map<String, String> home() {
        return Map.of("message", "Welcome, authenticated user!");
    }
}