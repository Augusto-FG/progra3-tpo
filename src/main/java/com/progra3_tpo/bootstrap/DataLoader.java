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

            LocationDto depositoCentral = new LocationDto("Depósito Central", "DEPOSITO", "Av. Principal 123");
            LocationDto depositoNorte = new LocationDto("Depósito Norte", "DEPOSITO", "Av. Norte 500");
            LocationDto depositoSur = new LocationDto("Depósito Sur", "DEPOSITO", "Av. Sur 420");
            LocationDto distribuidorX = new LocationDto("Distribuidor Leonel", "DISTRIBUIDOR", "Ruta 5 km 42");
            LocationDto distribuidorY = new LocationDto("Distribuidor Rojo", "DISTRIBUIDOR", "Camino Y 77");
            LocationDto distribuidorZ = new LocationDto("Distribuidor Meli", "DISTRIBUIDOR", "Camino Z 88");
            LocationDto clienteA = new LocationDto("Cliente Javier", "CLIENTE", "Calle 1");
            LocationDto clienteB = new LocationDto("Cliente Bravo", "CLIENTE", "Calle 2");
            LocationDto clienteJuan = new LocationDto("Cliente Juan", "CLIENTE", "Calle Juan 10");
            LocationDto clientePedro = new LocationDto("Cliente Pedro", "CLIENTE", "Calle Pedro 20");
            LocationDto clienteCarlos = new LocationDto("Cliente Carlos", "CLIENTE", "Calle Carlos 30");

            depositoCentral = locationRepository.save(depositoCentral);
            depositoNorte = locationRepository.save(depositoNorte);
            depositoSur = locationRepository.save(depositoSur);
            distribuidorX = locationRepository.save(distribuidorX);
            distribuidorY = locationRepository.save(distribuidorY);
            distribuidorZ = locationRepository.save(distribuidorZ);
            clienteA = locationRepository.save(clienteA);
            clienteB = locationRepository.save(clienteB);
            clienteJuan = locationRepository.save(clienteJuan);
            clientePedro = locationRepository.save(clientePedro);
            clienteCarlos = locationRepository.save(clienteCarlos); // persistir nuevo cliente

            // Nivel 1: Depósito Central -> depósitos secundarios
            RouteDto r1 = new RouteDto("Ruta Central-Norte", 10.0, 20.0, "AUTOPISTA", depositoNorte);
            RouteDto r2 = new RouteDto("Ruta Central-Sur", 12.0, 25.0, "AUTOPISTA", depositoSur);

            // Nivel 2: Depósito Norte -> distribuidores y clientes
            RouteDto r3 = new RouteDto("Camino Norte-X", 6.0, 12.0, "RURAL", distribuidorX);
            RouteDto r4 = new RouteDto("Camino Norte-Juan", 8.0, 16.0, "URBANO", clienteJuan);

            // Nivel 3: Distribuidor Leonel -> clientes
            RouteDto r5 = new RouteDto("Ruta X-ClienteA", 4.0, 8.0, "URBANO", clienteA);
            RouteDto r6 = new RouteDto("Ruta X-ClienteB", 5.0, 10.0, "URBANO", clienteB);

            // Nivel 2: Depósito Sur -> distribuidores
            RouteDto r7 = new RouteDto("Ruta Sur-Y", 7.0, 14.0, "RURAL", distribuidorY);

            // Nivel 3: Distribuidor Rojo -> Z -> Cliente Pedro
            RouteDto r8 = new RouteDto("Ruta Y-Z", 6.0, 12.0, "RURAL", distribuidorZ);
            RouteDto r9 = new RouteDto("Camino Z-Pedro", 3.0, 6.0, "URBANO", clientePedro);

            // Nuevas rutas solicitadas anteriormente:
            // Cliente Juan -> Cliente Pedro
            RouteDto r10 = new RouteDto("Camino Juan-Pedro", 6.0, 12.0, "URBANO", clientePedro);
            // Depósito Sur -> Cliente Juan
            RouteDto r11 = new RouteDto("Ruta Sur-Juan", 9.0, 18.0, "RURAL", clienteJuan);
            // Cliente Juan -> Cliente Bravo
            RouteDto r12 = new RouteDto("Camino Juan-B", 7.5, 14.0, "URBANO", clienteB);

            // Rutas nuevas hacia el nuevo cliente (desde Distribuidor Rojo y Distribuidor Meli)
            RouteDto r13 = new RouteDto("Camino Y-Carlos", 5.5, 11.0, "URBANO", clienteCarlos); // Distribuidor Rojo -> Cliente Carlos
            RouteDto r14 = new RouteDto("Ruta Z-Carlos", 8.0, 16.0, "RURAL", clienteCarlos);   // Distribuidor Meli -> Cliente Carlos

            //rutas salientes (añadir las nuevas rutas a los distribuidores correspondientes)
            depositoCentral.setRutas(List.of(r1, r2));
            depositoNorte.setRutas(List.of(r3, r4));
            depositoSur.setRutas(List.of(r7, r11));
            distribuidorX.setRutas(List.of(r5, r6));
            distribuidorY.setRutas(List.of(r8, r13));
            distribuidorZ.setRutas(List.of(r9, r14));
            clienteJuan.setRutas(List.of(r10, r12));

            // relaciones (persistir orígenes para que Neo4j cree las aristas)
            locationRepository.save(depositoCentral);
            locationRepository.save(depositoNorte);
            locationRepository.save(depositoSur);
            locationRepository.save(distribuidorX);
            locationRepository.save(distribuidorY);
            locationRepository.save(distribuidorZ);
            locationRepository.save(clienteJuan);

            System.out.println("✅ Base de datos inicializada con datos de ejemplo.");
        }
    }
}
