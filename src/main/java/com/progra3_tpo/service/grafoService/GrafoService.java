package com.progra3_tpo.service.grafoService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrafoService {

    private final LocationRepository locationRepository;
    private final DijkstraService dijkstraService;

    // Constructor: inyecta los servicios necesarios
    public GrafoService(LocationRepository locationRepository, DijkstraService dijkstraService) {
        this.locationRepository = locationRepository;
        this.dijkstraService = dijkstraService;
    }

    // Método principal que arma el grafo y ejecuta Dijkstra
    public PathResponse computeWithDijkstra(String from, String to, String metric, double alpha) {
        // 1. Obtiene todas las ubicaciones (nodos del grafo)
        List<LocationDto> nodes = locationRepository.findAll();
        int n = nodes.size();

        // 2. Crea un mapa para relacionar el nombre de cada ubicación con un índice numérico
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            nameToIndex.put(nodes.get(i).getNombre(), i);
        }

        // 3. Inicializa una lista de adyacencia: una lista de listas que representa el grafo
        List<List<DijkstraService.EdgeDto>> adjList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjList.add(new ArrayList<>());

        // 4. Recorre cada ubicación para construir las conexiones (aristas)
        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = src.getRutas(); // Rutas salientes desde esta ubicación
            if (rutas == null) continue; // Si no tiene rutas, pasa al siguiente nodo

            // 5. Por cada ruta, obtiene el nodo destino y agrega la arista al grafo
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;

                // Busca el índice del nodo destino
                Integer destIdx = nameToIndex.get(r.getDestino().getNombre());
                if (destIdx == null) continue;

                // Agrega una nueva arista (con distancia y costo) a la lista de adyacencia
                adjList.get(i).add(
                        new DijkstraService.EdgeDto(destIdx, r.getDistancia(), r.getCosto(), r)
                );
            }
        }

        // 6. Busca los índices de los nodos de origen y destino
        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);

        // 7. Si alguno no existe, devuelve una respuesta vacía con un mensaje de error
        if (s == null || t == null) {
            return new PathResponse("Inicio o destino no encontrado", new ArrayList<>(), new ArrayList<>(), 0.0, 0.0);
        }

        // 8. Define el criterio de cálculo (por defecto, "distance") y el peso alpha
        String m = (metric == null) ? "distance" : metric;
        if (Double.isNaN(alpha)) alpha = 0.5;

        // 9. Llama al servicio de Dijkstra para calcular el mejor camino
        return dijkstraService.compute(s, t, adjList, nodes, m, alpha);
    }
}
