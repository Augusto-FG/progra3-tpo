package com.progra3_tpo.service.progradinamica;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrograDinamicaService {

    private final LocationRepository locationRepository;

    public PrograDinamicaService(LocationRepository locationRepository) {
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

        // Construir lista plana de aristas
        List<Edge> edges = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            LocationDto src = nodes.get(u);
            List<RouteDto> rutas = (src == null) ? null : src.getRutas();
            if (rutas == null) continue;
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer v = nameToIndex.get(r.getDestino().getNombre());
                if (v == null) continue;
                double d = r.getDistancia();
                double c = r.getCosto();
                if (Double.isNaN(d) || Double.isNaN(c) || d < 0.0 || c < 0.0) continue;
                edges.add(new Edge(u, v, d, c, r));
            }
        }

        PathCandidate byCost = dpPath(n, s, t, edges, true);
        PathCandidate byDistance = dpPath(n, s, t, edges, false);

        PathCandidate best = chooseBest(byCost, byDistance);
        if (best == null) {
            return new PathResponse("No existe un camino entre los nodos.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Mapear a nombres como en DijkstraService
        List<String> nodeNames = best.nodeIdx.stream()
                .map(idx -> {
                    String name = nodes.get(idx).getNombre();
                    return name == null ? "?" : name;
                })
                .collect(Collectors.toList());

        List<String> routeNames = best.edges.stream()
                .map(r -> r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()))
                .collect(Collectors.toList());

        // Calcular totales desde las aristas
        double totalDistance = 0.0;
        double totalCost = 0.0;
        for (RouteDto r : best.edges) {
            if (r != null) {
                totalDistance += r.getDistancia();
                totalCost += r.getCosto();
            }
        }

        return new PathResponse("Recorrido calculado exitosamente.", nodeNames, routeNames, totalDistance, totalCost);
    }

    private PathCandidate dpPath(int n, int s, int t, List<Edge> edges, boolean costFirst) {
        final double INF = Double.POSITIVE_INFINITY;
        double[] prim = new double[n];
        double[] sec = new double[n];
        int[] pred = new int[n];
        RouteDto[] predEdge = new RouteDto[n];

        Arrays.fill(prim, INF);
        Arrays.fill(sec, INF);
        Arrays.fill(pred, -1);
        Arrays.fill(predEdge, null);
        prim[s] = 0.0;
        sec[s] = 0.0;

        // Bellman-Ford style relaxations
        for (int it = 0; it < n - 1; it++) {
            boolean updated = false;
            for (Edge e : edges) {
                if (Double.isInfinite(prim[e.from])) continue;

                double wPrim = costFirst ? e.cost : e.distance;
                double wSec = costFirst ? e.distance : e.cost;

                double candPrim = prim[e.from] + wPrim;
                double candSec = sec[e.from] + wSec;

                if (better(candPrim, candSec, prim[e.to], sec[e.to])) {
                    prim[e.to] = candPrim;
                    sec[e.to] = candSec;
                    pred[e.to] = e.from;
                    predEdge[e.to] = e.route;
                    updated = true;
                }
            }
            if (!updated) break;
        }

        if (Double.isInfinite(prim[t])) return null;

        // Reconstruir camino
        LinkedList<Integer> pathIdx = new LinkedList<>();
        LinkedList<RouteDto> edgesPath = new LinkedList<>();
        for (int cur = t; cur != -1; cur = pred[cur]) {
            pathIdx.addFirst(cur);
            if (cur != s) {
                RouteDto re = predEdge[cur];
                if (re == null) return null;
                edgesPath.addFirst(re);
            }
        }

        return new PathCandidate(new ArrayList<>(pathIdx), new ArrayList<>(edgesPath));
    }

    private boolean better(double candPrim, double candSec, double curPrim, double curSec) {
        int c = Double.compare(candPrim, curPrim);
        if (c < 0) return true;
        if (c > 0) return false;
        return Double.compare(candSec, curSec) < 0;
    }

    private PathCandidate chooseBest(PathCandidate a, PathCandidate b) {
        if (a == null) return b;
        if (b == null) return a;

        // Calcular totales para comparar
        double aCost = a.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getCosto).sum();
        double bCost = b.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getCosto).sum();
        if (Double.compare(aCost, bCost) != 0) return aCost <= bCost ? a : b;

        double aDist = a.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getDistancia).sum();
        double bDist = b.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getDistancia).sum();
        return aDist <= bDist ? a : b;
    }

    private static class Edge {
        final int from;
        final int to;
        final double distance;
        final double cost;
        final RouteDto route;
        Edge(int from, int to, double distance, double cost, RouteDto route) {
            this.from = from; this.to = to; this.distance = distance; this.cost = cost; this.route = route;
        }
    }

    private static class PathCandidate {
        final List<Integer> nodeIdx;
        final List<RouteDto> edges;
        PathCandidate(List<Integer> nodeIdx, List<RouteDto> edges) {
            this.nodeIdx = nodeIdx; this.edges = edges;
        }
    }
}