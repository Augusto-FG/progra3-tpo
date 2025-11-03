package com.progra3_tpo.service.greedy;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GreedyService {

    private final LocationRepository locationRepository;

    public GreedyService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public PathResponse compute(String from, String to) {
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("Inicio o destino no encontrado", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            LocationDto nd = nodes.get(i);
            if (nd != null && nd.getNombre() != null) nameToIndex.put(nd.getNombre(), i);
        }

        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Inicio o destino no encontrado", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }
        if (s.equals(t)) {
            String name = nodes.get(s).getNombre() == null ? "?" : nodes.get(s).getNombre();
            return new PathResponse("Recorrido calculado exitosamente.", Collections.singletonList(name), Collections.emptyList(), 0.0, 0.0);
        }

        // Construir adyacencias
        List<List<Edge>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = (src == null) ? null : src.getRutas();
            if (rutas == null) continue;
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer v = nameToIndex.get(r.getDestino().getNombre());
                if (v == null) continue;
                double d = r.getDistancia();
                double c = r.getCosto();
                if (Double.isNaN(d) || Double.isNaN(c) || d < 0 || c < 0) continue;
                adj.get(i).add(new Edge(v, d, c, r));
            }
        }

        PathCandidate byCost = greedyPath(s, t, adj);
        PathCandidate byDistance = greedyPathDistanceFirst(s, t, adj);

        PathCandidate best = chooseBest(byCost, byDistance);
        if (best == null) {
            return new PathResponse("No existe un recorrido posible entre los nodos especificados.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Construir listas legibles
        List<String> nodeNames = best.nodeIdx.stream()
                .map(idx -> {
                    String name = nodes.get(idx).getNombre();
                    return name == null ? "?" : name;
                })
                .collect(Collectors.toList());

        List<String> routeNames = best.edges.stream()
                .map(r -> r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()))
                .collect(Collectors.toList());

        return new PathResponse(
                "Recorrido calculado exitosamente.",
                nodeNames,
                routeNames,
                best.totalDistance,
                best.totalCost
        );
    }

    // Greedy costo-primero (tie por distancia)
    private PathCandidate greedyPath(int s, int t, List<List<Edge>> adj) {
        int n = adj.size();
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        List<RouteDto> edges = new ArrayList<>();
        double totalD = 0.0, totalC = 0.0;

        int cur = s;
        visited[cur] = true;
        path.add(cur);

        while (cur != t) {
            List<Edge> out = adj.get(cur);
            Edge best = null;
            for (Edge e : out) {
                if (visited[e.to]) continue;
                if (best == null
                        || e.cost < best.cost
                        || (e.cost == best.cost && e.distance < best.distance)) {
                    best = e;
                }
            }
            if (best == null) return null; // atascado
            edges.add(best.route);
            totalD += best.distance;
            totalC += best.cost;
            cur = best.to;
            if (visited[cur]) return null; // ciclo
            visited[cur] = true;
            path.add(cur);
        }

        return new PathCandidate(path, edges, totalD, totalC);
    }

    // Greedy distancia-primero (tie por costo)
    private PathCandidate greedyPathDistanceFirst(int s, int t, List<List<Edge>> adj) {
        int n = adj.size();
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        List<RouteDto> edges = new ArrayList<>();
        double totalD = 0.0, totalC = 0.0;

        int cur = s;
        visited[cur] = true;
        path.add(cur);

        while (cur != t) {
            List<Edge> out = adj.get(cur);
            Edge best = null;
            for (Edge e : out) {
                if (visited[e.to]) continue;
                if (best == null
                        || e.distance < best.distance
                        || (e.distance == best.distance && e.cost < best.cost)) {
                    best = e;
                }
            }
            if (best == null) return null; // atascado
            edges.add(best.route);
            totalD += best.distance;
            totalC += best.cost;
            cur = best.to;
            if (visited[cur]) return null; // ciclo
            visited[cur] = true;
            path.add(cur);
        }

        return new PathCandidate(path, edges, totalD, totalC);
    }

    private PathCandidate chooseBest(PathCandidate a, PathCandidate b) {
        if (a == null) return b;
        if (b == null) return a;
        int c = Double.compare(a.totalCost, b.totalCost);
        if (c != 0) return c <= 0 ? a : b;
        int d = Double.compare(a.totalDistance, b.totalDistance);
        return d <= 0 ? a : b;
    }

    private static class Edge {
        final int to;
        final double distance;
        final double cost;
        final RouteDto route;
        Edge(int to, double distance, double cost, RouteDto route) {
            this.to = to; this.distance = distance; this.cost = cost; this.route = route;
        }
    }

    private static class PathCandidate {
        final List<Integer> nodeIdx;
        final List<RouteDto> edges;
        final double totalDistance;
        final double totalCost;
        PathCandidate(List<Integer> nodeIdx, List<RouteDto> edges, double totalDistance, double totalCost) {
            this.nodeIdx = nodeIdx; this.edges = edges; this.totalDistance = totalDistance; this.totalCost = totalCost;
        }
    }
}
