// java
package com.progra3_tpo.bootstrap;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final LocationRepository locationRepository;

    public DataLoader(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public void run(String... args) {
        if (locationRepository.count() == 0) {

            LocationDto deposito = new LocationDto("Depósito Central", "DEPOSITO", "Av. Principal 123");
            LocationDto clienteA = new LocationDto("Cliente A", "CLIENTE", "Calle 1");
            LocationDto clienteB = new LocationDto("Cliente B", "CLIENTE", "Calle 2");
            LocationDto distribuidorX = new LocationDto("Distribuidor X", "DISTRIBUIDOR", "Ruta 5 km 42");

            deposito = locationRepository.save(deposito);
            clienteA = locationRepository.save(clienteA);
            clienteB = locationRepository.save(clienteB);
            distribuidorX = locationRepository.save(distribuidorX);

            // rutas desde los sources (outgoing)
            RouteDto ruta1 = new RouteDto(null, "Ruta Nacional 5", 5.0, 10.0, "URBANO", clienteA);
            RouteDto ruta2 = new RouteDto(null, "Autopista 25 de Mayo", 8.0, 15.0, "AUTOPISTA", clienteB);
            RouteDto ruta3 = new RouteDto(null, "Camino Rural A-X", 7.0, 12.0, "RURAL", distribuidorX);
            RouteDto ruta4 = new RouteDto(null, "Camino Rural B-X", 4.5, 9.0, "RURAL", distribuidorX);

            deposito.setRutas(List.of(ruta1, ruta2));
            clienteA.setRutas(List.of(ruta3));
            clienteB.setRutas(List.of(ruta4));

            // rutas entrantes en los destinos (cada RouteDto apunta al source)
            // para que SDN muestre las relaciones INCOMING en los destinos, el RouteDto debe apuntar al nodo origen
            clienteA.setRutasEntrantes(List.of(new RouteDto(null, "Ruta Nacional 5", 5.0, 10.0, "URBANO", deposito)));
            clienteB.setRutasEntrantes(List.of(new RouteDto(null, "Autopista 25 de Mayo", 8.0, 15.0, "AUTOPISTA", deposito)));
            distribuidorX.setRutasEntrantes(List.of(
                    new RouteDto(null, "Camino Rural A-X", 7.0, 12.0, "RURAL", clienteA),
                    new RouteDto(null, "Camino Rural B-X", 4.5, 9.0, "RURAL", clienteB)
            ));

            locationRepository.save(deposito);
            locationRepository.save(clienteA);
            locationRepository.save(clienteB);
            locationRepository.save(distribuidorX);

            System.out.println("✅ Datos de prueba cargados correctamente en Neo4j 🚀");
        }
    }
}
