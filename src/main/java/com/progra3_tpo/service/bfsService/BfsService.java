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

    // Metodo principal que aplica el algoritmo BFS para encontrar el camino con menos saltos (hops)
    public PathResponse computeBfsShortestHops(String from, String to) {

        // Validamos los datos de entrada
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return new PathResponse("Datos ingresados inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Obtenemos todos los nodos de la base de datos
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("No hay nodos en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>();

        // Se crea un mapa para acceder al índice de un nodo a partir de su nombre
        for (int i = 0; i < n; i++) {
            String nombre = nodes.get(i).getNombre();
            if (nombre != null) nameToIndex.put(nombre, i);
        }

        // Se buscan los índices del nodo origen y destino
        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Origen o destino no encontrados en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Se construye la lista de adyacencia (representación del grafo)
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        // Se recorren los nodos para agregar sus rutas de salida (sus conexiones)
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

        // ----------------------------------------------------------
        // ---------------------- SECCIÓN BFS ------------------------
        // ----------------------------------------------------------

        // Arreglos auxiliares para controlar qué nodos ya se visitaron y de dónde se llegó a cada uno
        boolean[] visited = new boolean[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);

        // Cola que se usará para recorrer los nodos en orden de niveles (propio del BFS)
        ArrayDeque<Integer> q = new ArrayDeque<>();
        visited[s] = true;
        q.add(s);

        boolean found = false;

        // Bucle principal del algoritmo BFS
        // Mientras haya nodos en la cola, se va recorriendo nivel por nivel
        while (!q.isEmpty()) {
            int u = q.poll(); // se saca el primer elemento de la cola
            if (u == t) { // si llegamos al destino, terminamos
                found = true;
                break;
            }
            // Recorremos todos los vecinos del nodo actual
            for (DijkstraService.EdgeDto e : adj.get(u)) {
                int v = e.to;
                if (!visited[v]) {
                    visited[v] = true; // se marca el nodo como visitado
                    parent[v] = u;     // se guarda de dónde se llegó a v
                    q.add(v);          // se agrega el nodo a la cola
                }
            }
        }

        // ----------------------------------------------------------
        // -------------------- FIN SECCIÓN BFS ---------------------
        // ----------------------------------------------------------

        // Si no se encontró camino, se devuelve un mensaje
        if (!found) {
            return new PathResponse("No existe recorrido entre origen y destino.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Reconstrucción del camino encontrado usando el arreglo 'parent'
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = t; at != -1; at = parent[at]) path.addFirst(at);

        // Se generan las listas de nombres de nodos y rutas que forman el recorrido
        List<String> nodeNames = new ArrayList<>();
        List<String> routeNames = new ArrayList<>();
        double totalDistance = 0.0;
        double totalCost = 0.0;

        // Se calcula la distancia y costo total del camino
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

        // Se devuelve la respuesta con la información completa del recorrido
        return new PathResponse("Recorrido calculado exitosamente.", nodeNames, routeNames, totalDistance, totalCost);
    }
}
