package com.progra3_tpo.validator;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathRequest;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PathRequestValidator {

    private final LocationRepository locationRepository;

    public PathRequestValidator(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Optional<PathResponse> validate(PathRequest req) {
        if (req == null || req.getFrom() == null || req.getTo() == null
                || req.getFrom().isBlank() || req.getTo().isBlank()) {
            return Optional.of(new PathResponse(
                    "Datos ingresados inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, 0.0
            ));
        }

        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return Optional.of(new PathResponse(
                    "No hay nodos en la base de datos.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, 0.0
            ));
        }

        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            String nombre = nodes.get(i).getNombre();
            if (nombre != null) nameToIndex.put(nombre, i);
        }

        Integer s = nameToIndex.get(req.getFrom());
        Integer t = nameToIndex.get(req.getTo());
        if (s == null || t == null) {
            return Optional.of(new PathResponse(
                    "Datos ingresados inválidos: origen o destino no encontrados en la base de datos.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, 0.0
            ));
        }

        int n = nodes.size();
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = src.getRutas();
            if (rutas == null) continue;
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer destIdx = nameToIndex.get(r.getDestino().getNombre());
                if (destIdx == null) continue;
                adj.get(i).add(new DijkstraService.EdgeDto(destIdx, r.getDistancia(), r.getCosto(), r));
            }
        }

        boolean hasEdges = adj.stream().anyMatch(list -> list != null && !list.isEmpty());
        if (!hasEdges) {
            return Optional.of(new PathResponse(
                    "No hay rutas cargadas en la base de datos.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, 0.0
            ));
        }

        // BFS para comprobar alcanzabilidad
        boolean[] visited = new boolean[n];
        Queue<Integer> q = new ArrayDeque<>();
        q.add(s);
        visited[s] = true;
        boolean reachable = false;
        while (!q.isEmpty()) {
            int u = q.poll();
            if (u == t) { reachable = true; break; }
            for (DijkstraService.EdgeDto e : adj.get(u)) {
                if (!visited[e.to]) {
                    visited[e.to] = true;
                    q.add(e.to);
                }
            }
        }
        if (!reachable) {
            return Optional.of(new PathResponse(
                    "No existe recorrido entre origen y destino.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, 0.0
            ));
        }

        return Optional.empty();
    }
}
