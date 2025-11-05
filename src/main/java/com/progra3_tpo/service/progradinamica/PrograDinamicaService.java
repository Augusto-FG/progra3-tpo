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

    // Método principal: busca el mejor recorrido entre dos nodos usando programación dinámica
    public PathResponse compute(String from, String to) {

        // Traemos todos los nodos de la base de datos
        List<LocationDto> nodes = locationRepository.findAll();
        if (nodes == null || nodes.isEmpty()) {
            return new PathResponse("No hay nodos cargados en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>(n);

        // Creamos un mapa que relaciona el nombre del nodo con su índice
        for (int i = 0; i < n; i++) {
            LocationDto nodo = nodes.get(i);
            if (nodo != null && nodo.getNombre() != null) {
                nameToIndex.put(nodo.getNombre(), i);
            }
        }

        // Obtenemos los índices del nodo de origen y destino
        Integer origen = nameToIndex.get(from);
        Integer destino = nameToIndex.get(to);

        // Validamos que ambos existan
        if (origen == null || destino == null) {
            return new PathResponse("Inicio o destino no encontrado.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Si origen y destino son el mismo, devolvemos un recorrido trivial
        if (origen.equals(destino)) {
            String name = nodes.get(origen).getNombre() == null ? "?" : nodes.get(origen).getNombre();
            return new PathResponse("Recorrido calculado exitosamente.",
                    Collections.singletonList(name), Collections.emptyList(), 0.0, 0.0);
        }

        // ------------------------------------------------------------
        // Construcción de la lista de aristas (todas las conexiones del grafo)
        // ------------------------------------------------------------
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = (src == null) ? null : src.getRutas();
            if (rutas == null) continue;

            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer destinoIdx = nameToIndex.get(r.getDestino().getNombre());
                if (destinoIdx == null) continue;

                double distancia = r.getDistancia();
                double costo = r.getCosto();

                // Evitamos valores inválidos o negativos
                if (Double.isNaN(distancia) || Double.isNaN(costo) || distancia < 0.0 || costo < 0.0) continue;

                edges.add(new Edge(i, destinoIdx, distancia, costo, r));
            }
        }

        // ------------------------------------------------------------
        // Aplicamos programación dinámica estilo Bellman-Ford
        // ------------------------------------------------------------

        // Buscamos el mejor camino por costo
        PathCandidate mejorPorCosto = calcularDP(n, origen, destino, edges, true);
        // Buscamos el mejor camino por distancia
        PathCandidate mejorPorDistancia = calcularDP(n, origen, destino, edges, false);

        // Elegimos el mejor entre ambos (según costo y distancia)
        PathCandidate mejorCamino = elegirMejor(mejorPorCosto, mejorPorDistancia);

        if (mejorCamino == null) {
            return new PathResponse("No existe un camino entre los nodos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // ------------------------------------------------------------
        // Armado de la respuesta (listado de nodos y rutas)
        // ------------------------------------------------------------

        // Convertimos índices de nodos a nombres
        List<String> nodeNames = mejorCamino.nodeIdx.stream()
                .map(idx -> {
                    String name = nodes.get(idx).getNombre();
                    return name == null ? "?" : name;
                })
                .collect(Collectors.toList());

        // Convertimos las aristas a nombres de rutas
        List<String> routeNames = mejorCamino.edges.stream()
                .map(r -> r == null ? "?" : (r.getNombreRuta() == null ? "?" : r.getNombreRuta()))
                .collect(Collectors.toList());

        // Calculamos la distancia y costo total del recorrido
        double totalDistance = 0.0;
        double totalCost = 0.0;
        for (RouteDto r : mejorCamino.edges) {
            if (r != null) {
                totalDistance += r.getDistancia();
                totalCost += r.getCosto();
            }
        }

        // Devolvemos el resultado final
        return new PathResponse("Recorrido calculado exitosamente.",
                nodeNames, routeNames, totalDistance, totalCost);
    }

    // ------------------------------------------------------------
    // Método que implementa la programación dinámica (Bellman-Ford)
    // ------------------------------------------------------------
    private PathCandidate calcularDP(int n, int origen, int destino, List<Edge> edges, boolean prioridadCosto) {
        final double INF = Double.POSITIVE_INFINITY;

        // prim y sec guardan los valores "principal" y "secundario" (costo o distancia)
        double[] prim = new double[n];
        double[] sec = new double[n];

        // pred guarda el nodo anterior en el camino más corto
        int[] pred = new int[n];
        // predEdge guarda la ruta usada para llegar a cada nodo
        RouteDto[] predEdge = new RouteDto[n];

        Arrays.fill(prim, INF);
        Arrays.fill(sec, INF);
        Arrays.fill(pred, -1);
        Arrays.fill(predEdge, null);

        // El nodo origen arranca con costo y distancia 0
        prim[origen] = 0.0;
        sec[origen] = 0.0;

        // Relajamos todas las aristas hasta n - 1 veces (método clásico de Bellman-Ford)
        for (int i = 0; i < n - 1; i++) {
            boolean actualizado = false;

            for (Edge e : edges) {
                if (Double.isInfinite(prim[e.from])) continue;

                // Dependiendo del parámetro prioridadCosto, se decide qué se minimiza primero
                double pesoPrincipal = prioridadCosto ? e.cost : e.distance;
                double pesoSecundario = prioridadCosto ? e.distance : e.cost;

                double nuevoPrim = prim[e.from] + pesoPrincipal;
                double nuevoSec = sec[e.from] + pesoSecundario;

                // Si encontramos un camino mejor, actualizamos los valores
                if (esMejor(nuevoPrim, nuevoSec, prim[e.to], sec[e.to])) {
                    prim[e.to] = nuevoPrim;
                    sec[e.to] = nuevoSec;
                    pred[e.to] = e.from;
                    predEdge[e.to] = e.route;
                    actualizado = true;
                }
            }

            // Si en una iteración no hubo cambios, el algoritmo puede detenerse
            if (!actualizado) break;
        }

        // Si el destino sigue con valor infinito, no hay camino posible
        if (Double.isInfinite(prim[destino])) return null;

        // Reconstruimos el camino desde el destino hasta el origen
        LinkedList<Integer> nodos = new LinkedList<>();
        LinkedList<RouteDto> rutas = new LinkedList<>();

        for (int actual = destino; actual != -1; actual = pred[actual]) {
            nodos.addFirst(actual);
            if (actual != origen) {
                RouteDto rutaAnterior = predEdge[actual];
                if (rutaAnterior == null) return null;
                rutas.addFirst(rutaAnterior);
            }
        }

        return new PathCandidate(new ArrayList<>(nodos), new ArrayList<>(rutas));
    }

    // Compara si un nuevo camino es mejor que el actual
    private boolean esMejor(double nuevoPrim, double nuevoSec, double actualPrim, double actualSec) {
        int cmp = Double.compare(nuevoPrim, actualPrim);
        if (cmp < 0) return true;
        if (cmp > 0) return false;
        return Double.compare(nuevoSec, actualSec) < 0;
    }

    // Decide cuál de los dos caminos candidatos es mejor
    private PathCandidate elegirMejor(PathCandidate a, PathCandidate b) {
        if (a == null) return b;
        if (b == null) return a;

        // Comparamos por costo total
        double costoA = a.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getCosto).sum();
        double costoB = b.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getCosto).sum();
        if (Double.compare(costoA, costoB) != 0) return costoA <= costoB ? a : b;

        // Si el costo es igual, se elige por distancia
        double distA = a.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getDistancia).sum();
        double distB = b.edges.stream().filter(Objects::nonNull).mapToDouble(RouteDto::getDistancia).sum();
        return distA <= distB ? a : b;
    }

    // ------------------------------------------------------------
    // Clases internas auxiliares
    // ------------------------------------------------------------

    // Representa una arista (conexión entre dos nodos del grafo)
    private static class Edge {
        final int from;
        final int to;
        final double distance;
        final double cost;
        final RouteDto route;

        Edge(int from, int to, double distance, double cost, RouteDto route) {
            this.from = from;
            this.to = to;
            this.distance = distance;
            this.cost = cost;
            this.route = route;
        }
    }

    // Representa una posible solución (lista de nodos y rutas del camino)
    private static class PathCandidate {
        final List<Integer> nodeIdx;
        final List<RouteDto> edges;

        PathCandidate(List<Integer> nodeIdx, List<RouteDto> edges) {
            this.nodeIdx = nodeIdx;
            this.edges = edges;
        }
    }
}
