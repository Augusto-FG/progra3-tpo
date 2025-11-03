package com.progra3_tpo.service.dikstraService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DijkstraService {

    // Esta clase interna se usa para representar una arista (conexión entre nodos)
    // Guarda el índice del nodo destino, la distancia, el costo y la ruta original.
    // La usan los servicios tipo Dijkstra, BFS, Backtracking, etc.
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

    // Esta clase Node es solo para manejar la cola de prioridad de Dijkstra
    // Guarda el índice del nodo y la distancia acumulada hasta ahí.
    private static class Node implements Comparable<Node> {
        final int idx;
        final double dist;
        Node(int idx, double dist) { this.idx = idx; this.dist = dist; }
        @Override
        public int compareTo(Node o) { return Double.compare(this.dist, o.dist); }
    }

    public PathResponse compute(int start, int goal, List<List<EdgeDto>> adjList, List<LocationDto> nodes, String metric, double alpha) {

        // Validamos que no haya datos nulos o índices fuera de rango
        if (adjList == null || nodes == null || start < 0 || goal < 0 || start >= adjList.size() || goal >= adjList.size()) {
            return new PathResponse("Parámetros inválidos", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Si el alpha viene fuera del rango (0 a 1), lo ajustamos
        if (alpha < 0.0) alpha = 0.0;
        if (alpha > 1.0) alpha = 1.0;
        String m = (metric == null) ? "distance" : metric.toLowerCase(Locale.ROOT);

        int n = adjList.size();
        double[] dist = new double[n];
        int[] prev = new int[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[start] = 0.0;

        // Cola de prioridad: siempre saca el nodo con menor distancia acumulada
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(start, 0.0));
        boolean[] visited = new boolean[n];

        // Acá arranca el algoritmo de Dijkstra posta
        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (visited[cur.idx]) continue;
            visited[cur.idx] = true;

            // Si llegamos al destino, ya está, cortamos
            if (cur.idx == goal) break;

            List<EdgeDto> edges = adjList.get(cur.idx);
            if (edges == null) continue;

            // Recorremos todos los vecinos del nodo actual
            for (EdgeDto e : edges) {
                int v = e.to;

                // Según la métrica elegida, usamos distancia, costo o combinación
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

                // Si el peso no tiene sentido, lo ignoramos
                if (Double.isNaN(w) || w < 0) continue;

                // Calculamos la nueva distancia acumulada
                double nd = dist[cur.idx] + w;

                // Si encontramos un camino más corto, lo actualizamos
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = cur.idx;
                    pq.add(new Node(v, nd));
                }
            }
        }

        // Si la distancia al destino sigue infinita, no hay forma de llegar
        if (Double.isInfinite(dist[goal])) {
            return new PathResponse("No existe un camino entre los nodos.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Reconstruimos el camino recorriendo los padres desde el destino hasta el origen
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = goal; at != -1; at = prev[at]) path.addFirst(at);

        // Armamos las listas con los nodos y rutas recorridas
        List<LocationDto> locationsOnPath = new ArrayList<>();
        List<RouteDto> routesOnPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            locationsOnPath.add(nodes.get(path.get(i)));
            if (i < path.size() - 1) {
                int u = path.get(i);
                int v = path.get(i + 1);
                Optional<EdgeDto> oe = adjList.get(u).stream().filter(e -> e.to == v).findFirst();
                oe.ifPresent(edgeDto -> {
                    if (edgeDto.route != null) routesOnPath.add(edgeDto.route);
                });
            }
        }

        // Acá pasamos la lista de objetos a nombres de nodos
        List<String> nodeNames = locationsOnPath.stream()
                .map(l -> l.getNombre() == null ? "?" : l.getNombre())
                .collect(Collectors.toList());

        // Y lo mismo pero con las rutas
        List<String> routeNamesList = routesOnPath.stream()
                .map(r -> r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()))
                .collect(Collectors.toList());

        // Calculamos las distancias y costos totales del recorrido
        double totalDistance = 0.0;
        double totalCost = 0.0;
        for (RouteDto r : routesOnPath) {
            if (r != null) {
                totalDistance += r.getDistancia();
                totalCost += r.getCosto();
            }
        }

        // Armamos el mensaje de respuesta final
        String message = "Recorrido calculado exitosamente.";

        // Y devolvemos todo encapsulado en el PathResponse
        return new PathResponse(message, nodeNames, routeNamesList, totalDistance, totalCost);
    }
}
