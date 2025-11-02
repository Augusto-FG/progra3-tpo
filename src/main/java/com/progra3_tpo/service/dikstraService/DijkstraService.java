package com.progra3_tpo.service.dikstraService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DijkstraService {

    public static class EdgeDto {
        public final int to;
        public final double distance;
        public final double cost;
        public final RouteDto route;

        public EdgeDto(int to, double distance, double cost, RouteDto route) {
            this.to = to;
            this.distance = distance;
            this.cost = cost;
            this.route = route;
        }
    }

    private static class Node implements Comparable<Node> {
        final int idx;
        final double dist;
        Node(int idx, double dist) { this.idx = idx; this.dist = dist; }
        @Override
        public int compareTo(Node o) { return Double.compare(this.dist, o.dist); }
    }

    public PathResponse compute(int start, int goal, List<List<EdgeDto>> adjList, List<LocationDto> nodes, String metric, double alpha) {
        if (adjList == null || nodes == null || start < 0 || goal < 0 || start >= adjList.size() || goal >= adjList.size()) {
            return new PathResponse("Invalid parameters", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }
        if (alpha < 0.0) alpha = 0.0;
        if (alpha > 1.0) alpha = 1.0;
        String m = (metric == null) ? "distance" : metric.toLowerCase(Locale.ROOT);

        int n = adjList.size();
        double[] dist = new double[n];
        int[] prev = new int[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[start] = 0.0;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(start, 0.0));
        boolean[] visited = new boolean[n];

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (visited[cur.idx]) continue;
            visited[cur.idx] = true;
            if (cur.idx == goal) break;

            List<EdgeDto> edges = adjList.get(cur.idx);
            if (edges == null) continue;
            for (EdgeDto e : edges) {
                int v = e.to;
                double w;
                switch (m) {
                    case "cost":
                        w = e.cost;
                        break;
                    case "weighted":
                        w = alpha * e.distance + (1.0 - alpha) * e.cost;
                        break;
                    case "distance":
                    default:
                        w = e.distance;
                        break;
                }
                if (Double.isNaN(w) || w < 0) continue;
                double nd = dist[cur.idx] + w;
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = cur.idx;
                    pq.add(new Node(v, nd));
                }
            }
        }

        if (Double.isInfinite(dist[goal])) {
            return new PathResponse("No path between nodes", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // reconstruye indices del camino
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = goal; at != -1; at = prev[at]) path.addFirst(at);

        // reconstruye LocationDto list y RouteDto list en orden
        List<LocationDto> locationsOnPath = new ArrayList<>();
        List<RouteDto> routesOnPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            locationsOnPath.add(nodes.get(path.get(i)));
            if (i < path.size() - 1) {
                int u = path.get(i);
                int v = path.get(i+1);
                Optional<EdgeDto> oe = adjList.get(u).stream().filter(e -> e.to == v).findFirst();
                oe.ifPresent(edgeDto -> {
                    if (edgeDto.route != null) routesOnPath.add(edgeDto.route);
                });
            }
        }

        //Esto convierte a lista de nombres de nodos
        List<String> nodeNames = locationsOnPath.stream()
                .map(l -> l.getNombre() == null ? "?" : l.getNombre())
                .collect(Collectors.toList());

        //Esto convierte a lista de nombres de rutas (para devolver en DijkstraResult)
        List<String> routeNamesList = routesOnPath.stream()
                .map(r -> r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()))
                .collect(Collectors.toList());

        // Calcular totales reales de distancia y costo
        double totalDistance = 0.0;
        double totalCost = 0.0;
        for (RouteDto r : routesOnPath) {
            if (r != null) {
                totalDistance += r.getDistancia();
                totalCost += r.getCosto();
            }
        }

        // Acá construimos cadenas con nombres de locations y routes (para el message)
        String locationNames = String.join(" -> ", nodeNames);
        String routeNames = String.join(" -> ", routeNamesList);

        String message = String.format("Recorrido calculado exitosamente.");

        //Acá devolvemos resultado usando los nuevos nombres de propiedades
        return new PathResponse(message, nodeNames, routeNamesList, totalDistance, totalCost);
    }
}
