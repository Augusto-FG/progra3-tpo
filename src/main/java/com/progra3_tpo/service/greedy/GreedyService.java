package com.progra3_tpo.service.greedy;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GreedyService {

    private final LocationRepository locationRepository;

    public GreedyService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public PathResponse compute(String from, String to) {

        // Validaciones básicas de entrada
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return new PathResponse("Datos inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Trae todas las localizaciones desde la base
        List<LocationDto> locations = locationRepository.findAll();
        if (locations == null || locations.isEmpty()) {
            return new PathResponse("No hay localizaciones cargadas en la base.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Mapea los nombres de las localizaciones a sus índices
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < locations.size(); i++) {
            String nombre = locations.get(i).getNombre();
            if (nombre != null) {
                nameToIndex.put(nombre, i);
            }
        }

        Integer origen = nameToIndex.get(from);
        Integer destino = nameToIndex.get(to);

        if (origen == null || destino == null) {
            return new PathResponse("Origen o destino no encontrados en la base.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Arma la lista de adyacencia (las conexiones entre nodos)
        List<List<DijkstraService.EdgeDto>> adyacencia = construirAdyacencia(locations, nameToIndex);

        // Variables para el recorrido
        boolean[] visitado = new boolean[locations.size()];
        List<String> nodosRecorridos = new ArrayList<>();
        List<String> rutasRecorridas = new ArrayList<>();
        double distanciaTotal = 0.0;
        double costoTotal = 0.0;

        int actual = origen;
        visitado[actual] = true;
        nodosRecorridos.add(obtenerNombre(locations.get(actual).getNombre()));

        // Algoritmo greedy: en cada paso elige la arista más barata
        while (actual != destino) {
            DijkstraService.EdgeDto mejorOpcion = seleccionarMejorRuta(adyacencia, actual, destino, visitado);

            if (mejorOpcion == null) {
                // No hay camino válido al destino
                return new PathResponse("No existe recorrido posible entre origen y destino.",
                        Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
            }

            int siguiente = mejorOpcion.to;
            visitado[siguiente] = true;

            String nombreSiguiente = obtenerNombre(locations.get(siguiente).getNombre());
            String nombreRuta = obtenerNombre(mejorOpcion.route != null ? mejorOpcion.route.getNombreRuta() : null);

            nodosRecorridos.add(nombreSiguiente);
            rutasRecorridas.add(nombreRuta);

            distanciaTotal += mejorOpcion.route != null ? mejorOpcion.route.getDistancia() : mejorOpcion.distance;
            costoTotal += mejorOpcion.route != null ? mejorOpcion.route.getCosto() : mejorOpcion.cost;

            actual = siguiente;
        }

        return new PathResponse("Recorrido calculado exitosamente.",
                nodosRecorridos, rutasRecorridas, distanciaTotal, costoTotal);
    }

    // Arma la lista de adyacencia a partir de las rutas de cada localización
    private List<List<DijkstraService.EdgeDto>> construirAdyacencia(List<LocationDto> locations, Map<String, Integer> nameToIndex) {
        int n = locations.size();
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            LocationDto origen = locations.get(i);
            List<RouteDto> rutas = origen.getRutas();
            if (rutas == null) continue;

            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;

                Integer idxDestino = nameToIndex.get(r.getDestino().getNombre());
                if (idxDestino != null) {
                    adj.get(i).add(new DijkstraService.EdgeDto(idxDestino, r.getDistancia(), r.getCosto(), r));
                }
            }
        }

        return adj;
    }

    // Selecciona la arista más barata disponible desde el nodo actual
    private DijkstraService.EdgeDto seleccionarMejorRuta(List<List<DijkstraService.EdgeDto>> adj, int actual, int destino, boolean[] visitado) {
        DijkstraService.EdgeDto mejor = null;
        double mejorCosto = Double.POSITIVE_INFINITY;
        double mejorDistancia = Double.POSITIVE_INFINITY;

        for (DijkstraService.EdgeDto e : adj.get(actual)) {
            if (visitado[e.to]) continue; // si ya fue visitado, lo salteo
            if (!puedeLlegar(adj, e.to, destino)) continue; // si no puede llegar al destino, lo descarto

            double costo = e.route != null ? e.route.getCosto() : e.cost;
            double distancia = e.route != null ? e.route.getDistancia() : e.distance;

            if (Double.isNaN(costo) || Double.isNaN(distancia)) continue;

            // Si el costo es menor, o si empata en costo pero tiene menor distancia, lo elijo
            if (costo < mejorCosto || (costo == mejorCosto && distancia < mejorDistancia)) {
                mejor = e;
                mejorCosto = costo;
                mejorDistancia = distancia;
            }
        }
        return mejor;
    }

    // BFS simple para comprobar si desde un nodo se puede llegar al destino
    private boolean puedeLlegar(List<List<DijkstraService.EdgeDto>> adj, int start, int goal) {
        if (start == goal) return true;

        boolean[] visto = new boolean[adj.size()];
        Queue<Integer> cola = new ArrayDeque<>();
        cola.add(start);
        visto[start] = true;

        while (!cola.isEmpty()) {
            int actual = cola.poll();
            for (DijkstraService.EdgeDto e : adj.get(actual)) {
                int prox = e.to;
                if (prox == goal) return true;
                if (!visto[prox]) {
                    visto[prox] = true;
                    cola.add(prox);
                }
            }
        }
        return false;
    }

    // Evita nulls o strings vacíos
    private String obtenerNombre(String nombre) {
        return (nombre == null || nombre.isBlank()) ? "?" : nombre;
    }
}
