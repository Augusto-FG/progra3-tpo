package com.progra3_tpo.service.backtrackingService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BacktrackingService {

    private final LocationRepository locationRepository;
    private final DijkstraService dijkstraService;
    private static final double EPS = 1e-9;

    public BacktrackingService(LocationRepository locationRepository, DijkstraService dijkstraService) {
        this.locationRepository = locationRepository;
        this.dijkstraService = dijkstraService;
    }

    public PathResponse computeOptimalPath(String from, String to, String metric, double alpha) {
        List<LocationDto> nodes = locationRepository.findAll();
        int n = nodes.size();
        if (n == 0) {
            return new PathResponse("Recorrido calculado exitosamente.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String nombre = nodes.get(i).getNombre();
            if (nombre != null) nameToIndex.put(nombre, i);
        }

        List<List<DijkstraService.EdgeDto>> adjList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjList.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = src.getRutas();
            if (rutas == null) continue;
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer destIdx = nameToIndex.get(r.getDestino().getNombre());
                if (destIdx == null) continue;
                adjList.get(i).add(new DijkstraService.EdgeDto(destIdx, r.getDistancia(), r.getCosto(), r));
            }
        }

        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Recorrido calculado exitosamente.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        Candidate best = new Candidate();

        // Acá añadimos soluciones de Dijkstra como candidatos iniciales (por costo y por distancia)
        try {
            PathResponse byCost = dijkstraService.compute(s, t, adjList, nodes, "cost", alpha);
            if (byCost != null && byCost.getAristasARecorrer() != null && !byCost.getAristasARecorrer().isEmpty()) {
                Candidate c = new Candidate(byCost.getNodosARecorrer(), byCost.getAristasARecorrer(), byCost.getTotalCost(), byCost.getTotalDistance());
                updateBestIfBetter(c, best);
            }
        } catch (Exception ignored) {}

        try {
            PathResponse byDistance = dijkstraService.compute(s, t, adjList, nodes, "distance", alpha);
            if (byDistance != null && byDistance.getAristasARecorrer() != null && !byDistance.getAristasARecorrer().isEmpty()) {
                Candidate c = new Candidate(byDistance.getNodosARecorrer(), byDistance.getAristasARecorrer(), byDistance.getTotalCost(), byDistance.getTotalDistance());
                updateBestIfBetter(c, best);
            }
        } catch (Exception ignored) {}

        // Backtracking: explorar caminos simples con poda por costo
        boolean[] visited = new boolean[n];
        List<String> curNodes = new ArrayList<>();
        List<String> curRoutes = new ArrayList<>();

        visited[s] = true;
        curNodes.add(nodes.get(s).getNombre());

        dfs(s, t, visited, curNodes, curRoutes, 0.0, 0.0, adjList, nodes, best);

        if (Double.isInfinite(best.totalCost)) {
            return new PathResponse("Recorrido calculado exitosamente.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        return new PathResponse("Recorrido calculado exitosamente.", best.nodeNames, best.routeNames, best.totalDistance, best.totalCost);
    }

    private void dfs(int u,
                     int target,
                     boolean[] visited,
                     List<String> curNodes,
                     List<String> curRoutes,
                     double curDist,
                     double curCost,
                     List<List<DijkstraService.EdgeDto>> adjList,
                     List<LocationDto> nodes,
                     Candidate best) {

        if (u == target) {
            Candidate cand = new Candidate(new ArrayList<>(curNodes), new ArrayList<>(curRoutes), curCost, curDist);
            updateBestIfBetter(cand, best);
            return;
        }

        if (!Double.isInfinite(best.totalCost) && curCost > best.totalCost + EPS) return;

        List<DijkstraService.EdgeDto> edges = adjList.get(u);
        if (edges == null) return;

        for (DijkstraService.EdgeDto e : edges) {
            int v = e.to;
            if (visited[v]) continue;

            RouteDto r = e.route;
            double d = (r == null) ? e.distance : r.getDistancia();
            double c = (r == null) ? e.cost : r.getCosto();
            String routeName = (r == null || r.getNombreRuta() == null) ? "?" : r.getNombreRuta();
            String nodeName = (nodes.get(v).getNombre() == null) ? "?" : nodes.get(v).getNombre();

            if (!Double.isInfinite(best.totalCost) && curCost + c > best.totalCost + EPS) continue;

            visited[v] = true;
            curRoutes.add(routeName);
            curNodes.add(nodeName);

            dfs(v, target, visited, curNodes, curRoutes, curDist + d, curCost + c, adjList, nodes, best);

            visited[v] = false;
            curRoutes.remove(curRoutes.size() - 1);
            curNodes.remove(curNodes.size() - 1);
        }
    }

    private void updateBestIfBetter(Candidate candidate, Candidate best) {
        if (Double.isInfinite(best.totalCost)) {
            best.copyFrom(candidate);
            return;
        }
        if (candidate.totalCost < best.totalCost - EPS) {
            best.copyFrom(candidate);
            return;
        }
        if (Math.abs(candidate.totalCost - best.totalCost) <= EPS && candidate.totalDistance < best.totalDistance - EPS) {
            best.copyFrom(candidate);
        }
    }

    private static class Candidate {
        List<String> nodeNames = new ArrayList<>();
        List<String> routeNames = new ArrayList<>();
        double totalCost = Double.POSITIVE_INFINITY;
        double totalDistance = Double.POSITIVE_INFINITY;

        Candidate() {}

        Candidate(List<String> nodeNames, List<String> routeNames, double totalCost, double totalDistance) {
            this.nodeNames = new ArrayList<>(nodeNames);
            this.routeNames = new ArrayList<>(routeNames);
            this.totalCost = totalCost;
            this.totalDistance = totalDistance;
        }

        void copyFrom(Candidate other) {
            this.nodeNames = new ArrayList<>(other.nodeNames);
            this.routeNames = new ArrayList<>(other.routeNames);
            this.totalCost = other.totalCost;
            this.totalDistance = other.totalDistance;
        }
    }
}
