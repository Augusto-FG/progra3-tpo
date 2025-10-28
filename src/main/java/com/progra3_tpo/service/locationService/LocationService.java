package com.progra3_tpo.service.locationService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LocationDto createLocationWithIncomingRoutes(CreateLocationRequest req) {
        LocationDto nueva = new LocationDto(req.getNombre(), req.getTipo(), req.getDireccion());
        LocationDto saved = locationRepository.save(nueva);

        if (req.getIncomingRoutes() != null) {
            for (CreateLocationRequest.IncomingRouteRequest ir : req.getIncomingRoutes()) {
                if (ir.getSourceId() == null) continue;
                locationRepository.findById(ir.getSourceId()).ifPresent(source -> {
                    if (source.getRutas() == null) source.setRutas(new ArrayList<>());
                    RouteDto ruta = new RouteDto(
                            null,
                            ir.getNombreRuta(),
                            ir.getDistancia(),
                            ir.getCosto(),
                            ir.getTipoCamino(),
                            saved
                    );
                    source.getRutas().add(ruta);
                    locationRepository.save(source); // persiste la relación desde el source hacia saved
                });
            }
        }

        // recargar la entidad para que venga con las relaciones entrantes
        return locationRepository.findById(saved.getId()).orElse(saved);
    }

    // métodos existentes
    public LocationDto saveLocation(LocationDto location) {
        return locationRepository.save(location);
    }

    public java.util.List<LocationDto> getAllLocations() {
        return locationRepository.findAll();
    }
}