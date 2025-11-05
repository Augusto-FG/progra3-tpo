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
    private static final double EPS = 1e-9; // margen de error para comparar números con coma flotante

    public BacktrackingService(LocationRepository locationRepository, DijkstraService dijkstraService) {
        this.locationRepository = locationRepository;
        this.dijkstraService = dijkstraService;
    }

    // Método principal que calcula el mejor camino entre dos nodos
    public PathResponse computeOptimalPath(String origen, String destino, String metric, double alpha) {
        // Se obtienen todos los nodos almacenados en la base de datos
        List<LocationDto> nodos = locationRepository.findAll();

        if (nodos.isEmpty()) {
            // Si no hay nodos cargados, se devuelve una respuesta vacía
            return new PathResponse("No hay nodos cargados.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Se arma un mapa para asociar el nombre de cada nodo con su índice en la lista
        Map<String, Integer> nombreAIndice = armarMapaDeIndices(nodos);

        // Se obtiene el índice del nodo origen y destino a partir de sus nombres
        Integer idxOrigen = nombreAIndice.get(origen);
        Integer idxDestino = nombreAIndice.get(destino);

        if (idxOrigen == null || idxDestino == null) {
            // Si alguno de los nodos no existe, no se puede calcular el camino
            return new PathResponse("Origen o destino no encontrado.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Se construye la lista de adyacencia, que representa las conexiones entre nodos
        List<List<DijkstraService.EdgeDto>> listaAdyacencia = armarListaAdyacencia(nodos, nombreAIndice);

        // Se calcula un primer camino usando Dijkstra, que servirá como punto de partida
        Candidate mejorCamino = inicializarMejorCaminoConDijkstra(idxOrigen, idxDestino, listaAdyacencia, nodos, alpha);

        // Se exploran todos los caminos posibles con backtracking (búsqueda exhaustiva)
        explorarCaminosBacktracking(idxOrigen, idxDestino, listaAdyacencia, nodos, mejorCamino);

        // Si no se encontró ningún camino válido, se devuelve una respuesta vacía
        if (Double.isInfinite(mejorCamino.totalCost)) {
            return new PathResponse("No se encontró camino.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // Si se encontró un camino, se devuelve la información del recorrido
        return new PathResponse(
                "Recorrido calculado exitosamente.",
                mejorCamino.nodeNames,
                mejorCamino.routeNames,
                mejorCamino.totalDistance,
                mejorCamino.totalCost
        );
    }

    // -------------------- métodos auxiliares --------------------

    // Crea un mapa donde se asocia el nombre de cada nodo con su índice en la lista
    private Map<String, Integer> armarMapaDeIndices(List<LocationDto> nodos) {
        Map<String, Integer> mapa = new HashMap<>();
        for (int i = 0; i < nodos.size(); i++) {
            String nombre = nodos.get(i).getNombre();
            if (nombre != null) mapa.put(nombre, i);
        }
        return mapa;
    }

    // Construye la lista de adyacencia que representa las rutas salientes de cada nodo
    private List<List<DijkstraService.EdgeDto>> armarListaAdyacencia(List<LocationDto> nodos, Map<String, Integer> nombreAIndice) {
        int n = nodos.size();
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        // Se recorren los nodos para crear las conexiones (aristas)
        for (int i = 0; i < n; i++) {
            LocationDto origen = nodos.get(i);
            List<RouteDto> rutas = origen.getRutas();
            if (rutas == null) continue;

            for (RouteDto r : rutas) {
                // Se verifica que la ruta y su destino sean válidos
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;

                Integer idxDestino = nombreAIndice.get(r.getDestino().getNombre());
                if (idxDestino != null) {
                    // Se agrega la conexión (arista) a la lista de adyacencia
                    adj.get(i).add(new DijkstraService.EdgeDto(idxDestino, r.getDistancia(), r.getCosto(), r));
                }
            }
        }
        return adj;
    }

    // Usa Dijkstra para obtener una primera solución inicial, tanto por costo como por distancia
    private Candidate inicializarMejorCaminoConDijkstra(int origen, int destino,
                                                        List<List<DijkstraService.EdgeDto>> adjList,
                                                        List<LocationDto> nodos,
                                                        double alpha) {
        Candidate mejor = new Candidate();

        // Se ejecuta Dijkstra priorizando el costo
        try {
            PathResponse porCosto = dijkstraService.compute(origen, destino, adjList, nodos, "cost", alpha);
            if (porCosto != null && !porCosto.getAristasARecorrer().isEmpty()) {
                Candidate c = new Candidate(
                        porCosto.getNodosARecorrer(),
                        porCosto.getAristasARecorrer(),
                        porCosto.getTotalCost(),
                        porCosto.getTotalDistance()
                );
                actualizarMejorCamino(c, mejor);
            }
        } catch (Exception ignored) {}

        // Se ejecuta Dijkstra priorizando la distancia
        try {
            PathResponse porDistancia = dijkstraService.compute(origen, destino, adjList, nodos, "distance", alpha);
            if (porDistancia != null && !porDistancia.getAristasARecorrer().isEmpty()) {
                Candidate c = new Candidate(
                        porDistancia.getNodosARecorrer(),
                        porDistancia.getAristasARecorrer(),
                        porDistancia.getTotalCost(),
                        porDistancia.getTotalDistance()
                );
                actualizarMejorCamino(c, mejor);
            }
        } catch (Exception ignored) {}

        return mejor;
    }

    // Inicia la búsqueda por backtracking desde el nodo origen
    private void explorarCaminosBacktracking(int u, int target,
                                             List<List<DijkstraService.EdgeDto>> adjList,
                                             List<LocationDto> nodos,
                                             Candidate mejorCamino) {
        boolean[] visitados = new boolean[nodos.size()]; // array que marca qué nodos ya se visitaron
        List<String> caminoActual = new ArrayList<>();   // guarda los nombres de los nodos del camino actual
        List<String> rutasActuales = new ArrayList<>();  // guarda los nombres de las rutas del camino actual

        visitados[u] = true;
        caminoActual.add(nodos.get(u).getNombre());

        // Se llama al DFS recursivo para recorrer todas las rutas posibles
        dfs(u, target, visitados, caminoActual, rutasActuales, 0.0, 0.0, adjList, nodos, mejorCamino);
    }

    // Algoritmo DFS recursivo que explora todos los caminos posibles (backtracking)
    private void dfs(int u,
                     int target,
                     boolean[] visitados,
                     List<String> caminoActual,
                     List<String> rutasActuales,
                     double distanciaAcum,
                     double costoAcum,
                     List<List<DijkstraService.EdgeDto>> adjList,
                     List<LocationDto> nodos,
                     Candidate mejorCamino) {

        // Si llegamos al destino, se crea un candidato con el camino recorrido
        if (u == target) {
            Candidate cand = new Candidate(new ArrayList<>(caminoActual), new ArrayList<>(rutasActuales), costoAcum, distanciaAcum);
            actualizarMejorCamino(cand, mejorCamino);
            return;
        }

        // Si el costo actual ya es mayor que el mejor encontrado, se corta la rama (poda)
        if (!Double.isInfinite(mejorCamino.totalCost) && costoAcum > mejorCamino.totalCost + EPS) return;

        // Se recorren todas las rutas salientes del nodo actual
        for (DijkstraService.EdgeDto e : adjList.get(u)) {
            int v = e.to;
            if (visitados[v]) continue; // se evita volver a un nodo ya visitado

            RouteDto r = e.route;
            double d = (r == null) ? e.distance : r.getDistancia();
            double c = (r == null) ? e.cost : r.getCosto();
            String ruta = (r == null || r.getNombreRuta() == null) ? "?" : r.getNombreRuta();
            String nodo = (nodos.get(v).getNombre() == null) ? "?" : nodos.get(v).getNombre();

            // Si el nuevo costo supera al mejor camino, se evita seguir explorando
            if (!Double.isInfinite(mejorCamino.totalCost) && costoAcum + c > mejorCamino.totalCost + EPS) continue;

            // Se marca el nodo como visitado y se agregan los datos actuales al camino
            visitados[v] = true;
            rutasActuales.add(ruta);
            caminoActual.add(nodo);

            // Llamada recursiva para seguir explorando desde el nuevo nodo
            dfs(v, target, visitados, caminoActual, rutasActuales, distanciaAcum + d, costoAcum + c, adjList, nodos, mejorCamino);

            // Se desmarcan los cambios para poder probar otros caminos (backtracking)
            visitados[v] = false;
            rutasActuales.remove(rutasActuales.size() - 1);
            caminoActual.remove(caminoActual.size() - 1);
        }
    }

    // Actualiza el mejor camino si el nuevo candidato es más conveniente
    private void actualizarMejorCamino(Candidate candidato, Candidate mejor) {
        if (Double.isInfinite(mejor.totalCost)) {
            mejor.copyFrom(candidato);
            return;
        }
        if (candidato.totalCost < mejor.totalCost - EPS) {
            mejor.copyFrom(candidato);
            return;
        }
        // Si el costo es igual, se elige el que tenga menor distancia
        if (Math.abs(candidato.totalCost - mejor.totalCost) <= EPS && candidato.totalDistance < mejor.totalDistance - EPS) {
            mejor.copyFrom(candidato);
        }
    }

    // Clase interna para representar un posible camino (candidato)
    private static class Candidate {
        List<String> nodeNames = new ArrayList<>();  // nombres de los nodos del camino
        List<String> routeNames = new ArrayList<>(); // nombres de las rutas recorridas
        double totalCost = Double.POSITIVE_INFINITY; // costo total del camino
        double totalDistance = Double.POSITIVE_INFINITY; // distancia total del camino

        Candidate() {}

        Candidate(List<String> nodeNames, List<String> routeNames, double totalCost, double totalDistance) {
            this.nodeNames = new ArrayList<>(nodeNames);
            this.routeNames = new ArrayList<>(routeNames);
            this.totalCost = totalCost;
            this.totalDistance = totalDistance;
        }

        // Copia los datos de otro candidato
        void copyFrom(Candidate other) {
            this.nodeNames = new ArrayList<>(other.nodeNames);
            this.routeNames = new ArrayList<>(other.routeNames);
            this.totalCost = other.totalCost;
            this.totalDistance = other.totalDistance;
        }
    }
}
