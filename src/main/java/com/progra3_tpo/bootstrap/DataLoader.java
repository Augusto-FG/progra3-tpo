package com.progra3_tpo.bootstrap;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final LocationRepository locationRepository;

    public DataLoader(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (locationRepository.count() == 0) {
            // crear ubicaciones sin rutas
            LocationDto deposito = new LocationDto(null, "Depósito Central", "DEPOSITO", "Av. Principal 123", null);
            LocationDto a = new LocationDto(null, "Cliente A", "CLIENTE", "Calle 1", null);
            LocationDto b = new LocationDto(null, "Cliente B", "CLIENTE", "Calle 2", null);
            LocationDto x = new LocationDto(null, "Distribuidor X", "DISTRIBUIDOR", "Ruta 5", null);

            // guardar nodos primero
            LocationDto savedDeposito = locationRepository.save(deposito);
            LocationDto savedA = locationRepository.save(a);
            LocationDto savedB = locationRepository.save(b);
            LocationDto savedX = locationRepository.save(x);

            // crear relaciones (RouteDto) apuntando a los destinos guardados
            RouteDto r1 = new RouteDto(null, 5.0, 10.0, savedA);
            RouteDto r2 = new RouteDto(null, 8.0, 15.0, savedB);
            RouteDto r3 = new RouteDto(null, 7.0, 12.0, savedX);
            RouteDto r4 = new RouteDto(null, 4.5, 9.0, savedX);

            // asignar rutas a los nodos origen y volver a guardar
            savedDeposito.setRutas(List.of(r1, r2));
            savedA.setRutas(List.of(r3));
            savedB.setRutas(List.of(r4));

            locationRepository.save(savedDeposito);
            locationRepository.save(savedA);
            locationRepository.save(savedB);
        }
    }
}
