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
            return new PathResponse("Inicio o destino no encontrado", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            LocationDto nd = nodes.get(i);
            if (nd != null && nd.getNombre() != null) {
                nameToIndex.put(nd.getNombre(), i);
            }
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

        // Build adjacency list
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
                if (Double.isNaN(d) || Double.isNaN(c) || d < 0.0 || c < 0.0) continue;
                adj.get(i).add(new Edge(v, d, c, r));
            }
        }

        boolean[] visited = new boolean[n];
        visited[s] = true;

        // Memo per strategy
        Map<Integer, PathCandidate> memoCost = new HashMap<>();
        Map<Integer, PathCandidate> memoDist = new HashMap<>();

        PathCandidate bestCostFirst = dfs(s, t, adj, visited, true, memoCost);
        PathCandidate bestDistFirst = dfs(s, t, adj, visited, false, memoDist);

        PathCandidate best = chooseBestOverall(bestCostFirst, bestDistFirst);
        if (best == null) {
            return new PathResponse("No existe un recorrido posible entre los nodos especificados.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Build response like DijkstraService (names only)
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

    // Divide & Conquer DFS with memoization
    private PathCandidate dfs(int u,
                              int t,
                              List<List<Edge>> adj,
                              boolean[] visited,
                              boolean costFirst,
                              Map<Integer, PathCandidate> memo) {
        if (u == t) {
            return new PathCandidate(new ArrayList<>(Collections.singletonList(u)), new ArrayList<>(), 0.0, 0.0);
        }

        // Use memo only when the state is "clean" (only current node visited)
        boolean onlyCurrentVisited = true;
        for (int i = 0; i < visited.length; i++) {
            if (i != u && visited[i]) { onlyCurrentVisited = false; break; }
        }
        if (onlyCurrentVisited && memo.containsKey(u)) {
            return memo.get(u);
        }

        List<Edge> out = adj.get(u);
        if (out == null || out.isEmpty()) return null;

        // Sort edges according to the chosen strategy to explore promising branches first
        out.sort(costFirst
                ? Comparator.comparingDouble((Edge e) -> e.cost).thenComparingDouble(e -> e.distance)
                : Comparator.comparingDouble((Edge e) -> e.distance).thenComparingDouble(e -> e.cost));

        PathCandidate best = null;

        for (Edge e : out) {
            if (visited[e.to]) continue;
            visited[e.to] = true;
            PathCandidate tail = dfs(e.to, t, adj, visited, costFirst, memo);
            visited[e.to] = false;

            if (tail == null) continue;

            // Combine current edge with tail
            List<Integer> nodeIdx = new ArrayList<>(1 + tail.nodeIdx.size());
            nodeIdx.add(u);
            nodeIdx.addAll(tail.nodeIdx);

            List<RouteDto> edges = new ArrayList<>(1 + tail.edges.size());
            edges.add(e.route);
            edges.addAll(tail.edges);

            double totalD = e.distance + tail.totalDistance;
            double totalC = e.cost + tail.totalCost;

            PathCandidate candidate = new PathCandidate(nodeIdx, edges, totalD, totalC);
            best = chooseByStrategy(best, candidate, costFirst);
        }

        if (onlyCurrentVisited && best != null) memo.put(u, best);
        return best;
    }

    private PathCandidate chooseByStrategy(PathCandidate a, PathCandidate b, boolean costFirst) {
        if (a == null) return b;
        if (b == null) return a;
        if (costFirst) {
            int c = Double.compare(a.totalCost, b.totalCost);
            if (c != 0) return c <= 0 ? a : b;
            int d = Double.compare(a.totalDistance, b.totalDistance);
            return d <= 0 ? a : b;
        } else {
            int d = Double.compare(a.totalDistance, b.totalDistance);
            if (d != 0) return d <= 0 ? a : b;
            int c = Double.compare(a.totalCost, b.totalCost);
            return c <= 0 ? a : b;
        }
    }

    private PathCandidate chooseBestOverall(PathCandidate a, PathCandidate b) {
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
            this.nodeIdx = nodeIdx;
            this.edges = edges;
            this.totalDistance = totalDistance;
            this.totalCost = totalCost;
        }
    }
}
