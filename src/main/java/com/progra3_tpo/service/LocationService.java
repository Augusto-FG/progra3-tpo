package com.progra3_tpo.service;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public LocationDto saveLocation(LocationDto location) {
        return locationRepository.save(location);
    }

    public List<LocationDto> getAllLocations() {
        return locationRepository.findAll();
    }
}
