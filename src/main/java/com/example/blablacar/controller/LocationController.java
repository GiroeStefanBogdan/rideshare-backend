package com.example.blablacar.controller;

import com.example.blablacar.dto.location.LocationResultDTO;
import com.example.blablacar.service.location.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public List<LocationResultDTO> search(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }
        return locationService.search(query.trim());
    }
}
