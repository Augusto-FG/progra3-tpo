package com.progra3_tpo.controller;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.service.locationService.CreateLocationRequest;
import com.progra3_tpo.service.locationService.LocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public LocationDto create(@RequestBody CreateLocationRequest request) {
        return locationService.createLocationWithIncomingRoutes(request);
    }

    @GetMapping
    public List<LocationDto> getAll() {
        return locationService.getAllLocations();
    }
}
