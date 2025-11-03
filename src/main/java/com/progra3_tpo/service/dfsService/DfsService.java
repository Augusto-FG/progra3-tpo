package com.progra3_tpo.service.dfsService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DfsService {

    private final LocationRepository locationRepository;
    private static final double EPS = 1e-9;

    public DfsService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    // Metodo principal: calcula un recorrido con DFS puro desde un nodo origen hasta un destino
    public PathResponse computeDfsPure(String from, String to) {

        // Validación básica de parámetros
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return new PathResponse("Datos ingresados inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Traemos todos los nodos guardados en la base de datos
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("No hay nodos en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Mapeamos los nombres de los nodos a índices (para trabajar más fácil)
        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String nombre = nodes.get(i).getNombre();
            if (nombre != null) nameToIndex.put(nombre, i);
        }

        // Obtenemos los índices de origen y destino
        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Origen o destino no encontrados en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Armamos la lista de adyacencia (todas las conexiones posibles)
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
                // Guardamos la arista con su destino, distancia y costo
                adj.get(i).add(new DijkstraService.EdgeDto(destIdx, r.getDistancia(), r.getCosto(), r));
            }
        }

        // Creamos una variable para guardar la mejor solución encontrada
        Candidate best = new Candidate();

        // Estructuras auxiliares para el recorrido
        boolean[] visited = new boolean[n];
        List<String> curNodes = new ArrayList<>();
        List<String> curRoutes = new ArrayList<>();

        // Marcamos el nodo de inicio como visitado y lo agregamos al recorrido actual
        visited[s] = true;
        curNodes.add(nodes.get(s).getNombre() == null ? "?" : nodes.get(s).getNombre());

        // Llamamos a la función recursiva DFS
        dfs(s, t, visited, curNodes, curRoutes, 0.0, 0.0, adj, nodes, best);

        // Si no se encontró ningún camino posible
        if (Double.isInfinite(best.totalCost)) {
            return new PathResponse("No existe recorrido entre origen y destino.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Si se encontró al menos un recorrido válido
        return new PathResponse("Recorrido calculado exitosamente.",
                best.nodeNames, best.routeNames, best.totalDistance, best.totalCost);
    }

    // DFS recursivo: explora todos los caminos posibles desde el nodo actual hasta el destino
    private void dfs(int u,
                     int target,
                     boolean[] visited,
                     List<String> curNodes,
                     List<String> curRoutes,
                     double curDist,
                     double curCost,
                     List<List<DijkstraService.EdgeDto>> adj,
                     List<LocationDto> nodes,
                     Candidate best) {

        // Caso base: llegamos al destino
        if (u == target) {
            Candidate cand = new Candidate(new ArrayList<>(curNodes), new ArrayList<>(curRoutes), curCost, curDist);
            updateBestIfBetter(cand, best); // actualiza la mejor solución si corresponde
            return;
        }

        // Poda: si el costo actual ya supera al mejor encontrado, no seguimos
        if (!Double.isInfinite(best.totalCost) && curCost > best.totalCost + EPS) return;

        List<DijkstraService.EdgeDto> edges = adj.get(u);
        if (edges == null || edges.isEmpty()) return;

        // Recorremos todos los vecinos del nodo actual
        for (DijkstraService.EdgeDto e : edges) {
            int v = e.to;
            if (visited[v]) continue; // evitamos ciclos

            RouteDto r = e.route;
            double d = (r == null) ? e.distance : r.getDistancia();
            double c = (r == null) ? e.cost : r.getCosto();
            String routeName = (r == null || r.getNombreRuta() == null) ? "?" : r.getNombreRuta();
            String nodeName = (nodes.get(v).getNombre() == null) ? "?" : nodes.get(v).getNombre();

            // Poda adicional: si el costo acumulado ya es mayor al mejor conocido, se corta
            if (!Double.isInfinite(best.totalCost) && curCost + c > best.totalCost + EPS) continue;

            // Marcamos el nodo como visitado y avanzamos
            visited[v] = true;
            curRoutes.add(routeName);
            curNodes.add(nodeName);

            // llamada recursiva
            dfs(v, target, visited, curNodes, curRoutes, curDist + d, curCost + c, adj, nodes, best);

            // backtracking: deshacemos los pasos para probar otros caminos
            visited[v] = false;
            curRoutes.remove(curRoutes.size() - 1);
            curNodes.remove(curNodes.size() - 1);
        }
    }

    // Si el nuevo camino es mejor, lo guardamos
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

    // Clase interna para guardar la mejor solución encontrada hasta el momento
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
