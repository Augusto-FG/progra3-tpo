package com.progra3_tpo.service.divideyconquista;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DivideyConquistaService {

    private final LocationRepository locationRepository;

    public DivideyConquistaService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public PathResponse compute(String from, String to) {
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("Inicio o destino no encontrado",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Mapeamos los nombres de las ubicaciones a índices
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            LocationDto n = nodes.get(i);
            if (n != null && n.getNombre() != null)
                nameToIndex.put(n.getNombre(), i);
        }

        Integer start = nameToIndex.get(from);
        Integer end = nameToIndex.get(to);
        if (start == null || end == null) {
            return new PathResponse("Inicio o destino no encontrado",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Creamos la lista de adyacencia
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) adj.add(new ArrayList<>());
        for (int i = 0; i < nodes.size(); i++) {
            LocationDto src = nodes.get(i);
            if (src.getRutas() == null) continue;
            for (RouteDto r : src.getRutas()) {
                if (r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer dest = nameToIndex.get(r.getDestino().getNombre());
                if (dest == null) continue;
                adj.get(i).add(new Edge(dest, r.getDistancia(), r.getCosto(), r));
            }
        }

        boolean[] visited = new boolean[nodes.size()];
        visited[start] = true;

        // Acá arrancamos el divide y conquista de verdad
        PathCandidate best = buscarCamino(start, end, adj, visited);

        if (best == null)
            return new PathResponse("No hay camino posible entre los nodos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);

        // Convertimos a nombres para la respuesta
        List<String> nodeNames = best.nodeIdx.stream()
                .map(i -> nodes.get(i).getNombre())
                .collect(Collectors.toList());

        List<String> routeNames = best.edges.stream()
                .map(r -> r.getNombreRuta())
                .collect(Collectors.toList());

        return new PathResponse(
                "Recorrido calculado exitosamente.",
                nodeNames,
                routeNames,
                best.totalDistance,
                best.totalCost
        );
    }

    // ---- Divide y conquista puro ----
    private PathCandidate buscarCamino(int actual, int destino, List<List<Edge>> adj, boolean[] visited) {
        // Caso base: si ya llegamos al destino, devolvemos un camino vacío
        if (actual == destino) {
            return new PathCandidate(
                    new ArrayList<>(List.of(actual)),
                    new ArrayList<>(),
                    0.0,
                    0.0
            );
        }

        List<Edge> aristas = adj.get(actual);
        if (aristas == null || aristas.isEmpty()) return null;

        PathCandidate mejor = null;

        // Recorremos cada arista saliente
        for (Edge e : aristas) {
            if (visited[e.to]) continue;

            // Marcamos el nodo como visitado
            visited[e.to] = true;

            // DIVIDIMOS: exploramos recursivamente el subcamino desde el siguiente nodo
            PathCandidate subcamino = buscarCamino(e.to, destino, adj, visited);

            // Desmarcamos para probar otras rutas
            visited[e.to] = false;

            // Si no hay camino por esa arista, seguimos con la siguiente
            if (subcamino == null) continue;

            // CONQUISTAMOS: combinamos este tramo con el resto del camino
            List<Integer> nuevosNodos = new ArrayList<>();
            nuevosNodos.add(actual);
            nuevosNodos.addAll(subcamino.nodeIdx);

            List<RouteDto> nuevasRutas = new ArrayList<>();
            nuevasRutas.add(e.route);
            nuevasRutas.addAll(subcamino.edges);

            double totalDist = e.distance + subcamino.totalDistance;
            double totalCost = e.cost + subcamino.totalCost;

            PathCandidate candidato = new PathCandidate(nuevosNodos, nuevasRutas, totalDist, totalCost);

            // Elegimos el mejor (el de menor costo, y si empatan, el de menor distancia)
            if (mejor == null
                    || totalCost < mejor.totalCost
                    || (totalCost == mejor.totalCost && totalDist < mejor.totalDistance)) {
                mejor = candidato;
            }
        }

        return mejor;
    }

    // ----- Clases auxiliares -----
    private static class Edge {
        int to;
        double distance;
        double cost;
        RouteDto route;
        Edge(int to, double distance, double cost, RouteDto route) {
            this.to = to;
            this.distance = distance;
            this.cost = cost;
            this.route = route;
        }
    }

    private static class PathCandidate {
        List<Integer> nodeIdx;
        List<RouteDto> edges;
        double totalDistance;
        double totalCost;
        PathCandidate(List<Integer> nodeIdx, List<RouteDto> edges, double totalDistance, double totalCost) {
            this.nodeIdx = nodeIdx;
            this.edges = edges;
            this.totalDistance = totalDistance;
            this.totalCost = totalCost;
        }
    }
}
