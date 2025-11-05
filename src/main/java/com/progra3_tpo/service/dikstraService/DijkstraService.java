package com.progra3_tpo.service.dikstraService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DijkstraService {

    // Clase interna que representa una conexión (arista) entre dos nodos.
    // Contiene el índice del nodo destino, la distancia, el costo y la ruta asociada.
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

    // Clase usada para manejar la cola de prioridad del algoritmo.
    // Guarda el índice del nodo y la distancia acumulada desde el origen hasta ese punto.
    private static class Node implements Comparable<Node> {
        final int index;
        final double distance;

        Node(int index, double distance) {
            this.index = index;
            this.distance = distance;
        }

        // Permite que la cola de prioridad ordene los nodos por distancia mínima.
        @Override
        public int compareTo(Node o) {
            return Double.compare(this.distance, o.distance);
        }
    }

    // Metodo principal que ejecuta el algoritmo de Dijkstra.
    // Calcula el camino más corto entre un nodo de origen y uno de destino
    // dentro de un grafo ponderado, según la métrica elegida.
    public PathResponse compute(int start, int goal, List<List<EdgeDto>> adjList, List<LocationDto> nodes, String metric, double alpha) {

        // Validación básica de los parámetros recibidos
        if (!isValidInput(start, goal, adjList, nodes))
            return new PathResponse("Parámetros inválidos", List.of(), List.of(), 0, 0);

        // Aseguramos que el valor de alpha esté entre 0 y 1
        alpha = Math.max(0, Math.min(1, alpha));

        // Definimos la métrica por defecto (distancia)
        metric = (metric == null) ? "distance" : metric.toLowerCase();

        //inicializacion

        int n = adjList.size(); // Número de nodos en el grafo
        double[] dist = new double[n]; // Distancias acumuladas desde el nodo inicial
        int[] prev = new int[n];       // Vector de predecesores (para reconstruir el camino)
        Arrays.fill(dist, Double.POSITIVE_INFINITY); // Inicialmente todas las distancias son infinitas
        Arrays.fill(prev, -1); // Inicialmente no hay predecesores
        dist[start] = 0; // La distancia al nodo inicial siempre es cero

        PriorityQueue<Node> pq = new PriorityQueue<>(); // Cola de prioridad que elige el nodo con la menor distancia acumulada
        pq.add(new Node(start, 0)); // agrega el vértice `start` con distancia 0 (origen)

        boolean[] visited = new boolean[n]; // Marca los nodos ya procesados

        // Bucle principal del algoritmo de Dijkstra
        while (!pq.isEmpty()) {//mientras la cola no esté vacía
            Node current = pq.poll(); // acá se obtiene y elimina el nodo con menor distancia
            if (visited[current.index]) continue;
            visited[current.index] = true;

            // Si llegamos al nodo destino, podemos cortar el proceso
            if (current.index == goal) break;

            // Relajacion, recorremos todas las aristas del nodo actual para actualizar distancias
            for (EdgeDto edge : adjList.get(current.index)) {
                double weight = calculateWeight(edge, metric, alpha);//caculamos el peso de cada arista
                if (weight < 0 || Double.isNaN(weight)) continue; // Rechaza NaN infinitos y también pesos negativos

                double newDist = dist[current.index] + weight;

                // Si encontramos un camino más corto hacia el nodo vecino, lo actualizamos
                if (newDist < dist[edge.to]) {//si la distancia calculada es menor a la guardada en el nodo
                    dist[edge.to] = newDist;
                    prev[edge.to] = current.index;
                    pq.add(new Node(edge.to, newDist));
                }
            }
        }

        // Si el destino no fue alcanzado, significa que no existe un camino posible
        if (Double.isInfinite(dist[goal]))
            return new PathResponse("No existe un camino entre los nodos.", List.of(), List.of(), 0, 0);

        // Si existe camino, reconstruimos el recorrido y armamos la respuesta
        return buildPathResponse(start, goal, prev, nodes, adjList);
    }


    // ======================= MÉTODOS AUXILIARES =======================

    // Verifica que los datos de entrada sean válidos antes de ejecutar el algoritmo
    private boolean isValidInput(int start, int goal, List<List<EdgeDto>> adjList, List<LocationDto> nodes) {
        return adjList != null && nodes != null &&
                start >= 0 && goal >= 0 &&
                start < adjList.size() && goal < adjList.size();
    }

    // Calcula el peso de una arista según la métrica elegida:
    // puede basarse en distancia, costo, o una combinación de ambas.
    private double calculateWeight(EdgeDto edge, String metric, double alpha) {
        return switch (metric) {
            case "cost" -> edge.cost;
            case "weighted" -> alpha * edge.distance + (1 - alpha) * edge.cost;
            default -> edge.distance;
        };
    }

    // Reconstruye el camino encontrado desde el nodo origen hasta el destino
    // y calcula las métricas finales de distancia y costo.
    private PathResponse buildPathResponse(int start, int goal, int[] prev,
                                           List<LocationDto> nodes, List<List<EdgeDto>> adjList) {

        // Reconstrucción del camino a partir del vector de predecesores
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = goal; at != -1; at = prev[at]) path.addFirst(at);

        // Obtenemos los objetos LocationDto y RouteDto correspondientes al recorrido
        List<LocationDto> locations = path.stream().map(nodes::get).toList();
        List<RouteDto> routes = getRoutesFromPath(path, adjList);

        // Calculamos la distancia y el costo total del recorrido
        double totalDistance = routes.stream().filter(Objects::nonNull)
                .mapToDouble(RouteDto::getDistancia).sum();
        double totalCost = routes.stream().filter(Objects::nonNull)
                .mapToDouble(RouteDto::getCosto).sum();

        // Listas de nombres de los nodos y rutas para la respuesta final
        List<String> nodeNames = locations.stream()
                .map(l -> Optional.ofNullable(l.getNombre()).orElse("?")).toList();

        List<String> routeNames = routes.stream()
                .map(r -> (r == null || r.getNombreRuta() == null) ? "?" : r.getNombreRuta())
                .toList();

        // Devuelve el resultado final del cálculo del camino
        return new PathResponse(
                "Recorrido calculado exitosamente.",
                nodeNames,
                routeNames,
                totalDistance,
                totalCost
        );
    }

    // Busca las rutas (aristas) que conectan los nodos del camino recorrido
    private List<RouteDto> getRoutesFromPath(List<Integer> path, List<List<EdgeDto>> adjList) {
        List<RouteDto> routes = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            int u = path.get(i);
            int v = path.get(i + 1);
            adjList.get(u).stream()
                    .filter(e -> e.to == v && e.route != null)
                    .findFirst()
                    .ifPresent(e -> routes.add(e.route));
        }
        return routes;
    }
}
