package com.progra3_tpo.service.bfsService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BfsService {

    private final LocationRepository locationRepository;

    public BfsService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public PathResponse computeBfsShortestHops(String from, String to) {

        // Validamos que los datos estén bien
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return new PathResponse("Datos ingresados inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Traemos todos los nodos de la base
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("No hay nodos en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>();

        // Armamos un map para acceder rápido al índice de cada nodo por nombre
        for (int i = 0; i < n; i++) {
            String nombre = nodes.get(i).getNombre();
            if (nombre != null) nameToIndex.put(nombre, i);
        }

        // Buscamos el índice del origen y destino
        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Origen o destino no encontrados en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Armamos la lista de adyacencia con las rutas
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        // Recorremos los nodos y guardamos sus conexiones
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

        // BFS puro: usamos cola y parent para reconstruir el camino
        boolean[] visited = new boolean[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        visited[s] = true;
        q.add(s);

        boolean found = false;
        while (!q.isEmpty()) {
            int u = q.poll();
            if (u == t) { found = true; break; }
            // Metemos los vecinos del nodo actual
            for (DijkstraService.EdgeDto e : adj.get(u)) {
                int v = e.to;
                if (!visited[v]) {
                    visited[v] = true;
                    parent[v] = u;
                    q.add(v);
                }
            }
        }

        // Si no se encontró ningún camino, devolvemos mensaje
        if (!found) {
            return new PathResponse("No existe recorrido entre origen y destino.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Reconstruimos el camino desde el destino hasta el origen
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = t; at != -1; at = parent[at]) path.addFirst(at);

        // Armamos las listas para mostrar nombres y rutas
        List<String> nodeNames = new ArrayList<>();
        List<String> routeNames = new ArrayList<>();
        double totalDistance = 0.0;
        double totalCost = 0.0;

        for (int i = 0; i < path.size(); i++) {
            nodeNames.add(nodes.get(path.get(i)).getNombre() == null ? "?" : nodes.get(path.get(i)).getNombre());
            if (i < path.size() - 1) {
                int u = path.get(i);
                int v = path.get(i + 1);
                Optional<DijkstraService.EdgeDto> oe = adj.get(u).stream().filter(e -> e.to == v).findFirst();
                if (oe.isPresent()) {
                    DijkstraService.EdgeDto edge = oe.get();
                    RouteDto r = edge.route;
                    routeNames.add(r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()));
                    totalDistance += r == null ? edge.distance : r.getDistancia();
                    totalCost += r == null ? edge.cost : r.getCosto();
                } else {
                    routeNames.add("?");
                }
            }
        }

        // Devolvemos el resultado final del recorrido
        return new PathResponse("Recorrido calculado exitosamente.", nodeNames, routeNames, totalDistance, totalCost);
    }
}
