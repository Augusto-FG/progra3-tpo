// language: java
package com.progra3_tpo.service.grafoService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Servicio de grafo que construye el grafo desde Neo4j y permite Dijkstra por nombre o por índice.
 */
@Service
public class GrafoService {

    private final LocationRepository locationRepository;

    private int V = 0;
    private LinkedList<Edge>[] adj;
    private final Map<String, Integer> nameToIndex = new HashMap<>();
    private final Map<Long, Integer> idToIndex = new HashMap<>();
    private final List<LocationDto> nodes = new ArrayList<>();

    public GrafoService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @PostConstruct
    private void init() {
        buildGraphFromDatabase();
    }

    private void buildGraphFromDatabase() {
        nodes.clear();
        nameToIndex.clear();
        idToIndex.clear();

        List<LocationDto> locations = locationRepository.findAll();
        V = locations.size();
        nodes.addAll(locations);

        if (V == 0) {
            adj = new LinkedList[0];
            return;
        }

        adj = new LinkedList[V];
        for (int i = 0; i < V; i++) adj[i] = new LinkedList<>();

        // asignar índices
        for (int i = 0; i < V; i++) {
            LocationDto loc = nodes.get(i);
            if (loc.getId() != null) idToIndex.put(loc.getId(), i);
            if (loc.getNombre() != null) nameToIndex.put(loc.getNombre(), i);
        }

        // agregar aristas usando RouteDto.costo (si está disponible)
        for (int i = 0; i < V; i++) {
            LocationDto src = nodes.get(i);
            if (src.getRutas() == null) continue;
            for (RouteDto r : src.getRutas()) {
                if (r == null || r.getDestino() == null) continue;
                Integer destIndex = null;
                if (r.getDestino().getId() != null) {
                    destIndex = idToIndex.get(r.getDestino().getId());
                }
                if (destIndex == null && r.getDestino().getNombre() != null) {
                    destIndex = nameToIndex.get(r.getDestino().getNombre());
                }
                if (destIndex != null) {
                    double peso = r.getCosto();
                    adj[i].add(new Edge(destIndex, peso));
                    // si el grafo fuera no dirigido, también agregar la inversa:
                    // adj[destIndex].add(new Edge(i, peso));
                }
            }
        }
    }

    private static class Edge {
        final int to;
        final double weight;
        Edge(int to, double weight) { this.to = to; this.weight = weight; }
    }

    private boolean enRango(int nodo) {
        return nodo >= 0 && nodo < V;
    }

    /**
     * Dijkstra por índice de inicio y nombre del destino.
     */
    public DijkstraResult dijkstra(int inicio, String nombreFin) {
        if (V == 0) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "No hay nodos cargados en el grafo");
        }
        Integer fin = nameToIndex.get(nombreFin);
        if (fin == null) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "Nombre de destino no encontrado: " + nombreFin);
        }
        if (!enRango(inicio) || !enRango(fin)) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "Índices fuera de rango");
        }
        if (inicio == fin) {
            return new DijkstraResult(List.of(inicio), 0.0, "Inicio y fin son iguales");
        }

        double[] dist = new double[V];
        int[] prev = new int[V];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[inicio] = 0.0;

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingDouble(i -> dist[i]));
        pq.add(inicio);

        boolean[] visited = new boolean[V];

        while (!pq.isEmpty()) {
            int u = pq.poll();
            if (visited[u]) continue;
            visited[u] = true;
            if (u == fin) break;

            for (Edge e : adj[u]) {
                int v = e.to;
                double alt = dist[u] + e.weight;
                if (alt < dist[v]) {
                    dist[v] = alt;
                    prev[v] = u;
                    pq.add(v);
                }
            }
        }

        if (prev[fin] == -1) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "No existe camino entre inicio y fin");
        }

        LinkedList<Integer> path = new LinkedList<>();
        for (int at = fin; at != -1; at = prev[at]) {
            path.addFirst(at);
            if (at == inicio) break;
        }
        if (path.isEmpty() || path.getFirst() != inicio) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "No existe camino entre inicio y fin");
        }

        double totalCost = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            int u = path.get(i);
            int v = path.get(i + 1);
            boolean found = false;
            for (Edge e : adj[u]) {
                if (e.to == v) {
                    totalCost += e.weight;
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Edge e : adj[v]) {
                    if (e.to == u) {
                        totalCost += e.weight;
                        break;
                    }
                }
            }
        }

        String msg = "Utilizando Dijkstra la ruta menos costosa a '" + nombreFin + "' es: " + path + " (costo=" + totalCost + ")";
        return new DijkstraResult(path, totalCost, msg);
    }

    /**
     * Dijkstra por nombre de inicio y nombre del destino (traduce nombre a índice).
     */
    public DijkstraResult dijkstra(String nombreInicio, String nombreFin) {
        if (V == 0) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "No hay nodos cargados en el grafo");
        }
        Integer inicioIdx = nameToIndex.get(nombreInicio);
        if (inicioIdx == null) {
            return new DijkstraResult(Collections.emptyList(), 0.0, "Nombre de inicio no encontrado: " + nombreInicio);
        }
        return dijkstra(inicioIdx, nombreFin);
    }

    // Métodos auxiliares (placeholders) - ajustar según necesidad
    public List<Integer> bfs(int inicio) { return Collections.emptyList(); }
    public List<Integer> dfs(int inicio) { return Collections.emptyList(); }
    public String prim() { return "No implementado"; }
    public String kruskal() { return "No implementado"; }
}
