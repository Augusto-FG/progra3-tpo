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
    private static final double EPS = 1e-9;

    public BacktrackingService(LocationRepository locationRepository, DijkstraService dijkstraService) {
        this.locationRepository = locationRepository;
        this.dijkstraService = dijkstraService;
    }

    // Método principal que calcula el mejor camino entre dos nodos
    public PathResponse computeOptimalPath(String origen, String destino, String metric, double alpha) {
        List<LocationDto> nodos = locationRepository.findAll();
        if (nodos.isEmpty()) {
            // si no hay nodos, devolvemos vacío
            return new PathResponse("No hay nodos cargados.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        Map<String, Integer> nombreAIndice = armarMapaDeIndices(nodos);
        Integer idxOrigen = nombreAIndice.get(origen);
        Integer idxDestino = nombreAIndice.get(destino);

        if (idxOrigen == null || idxDestino == null) {
            return new PathResponse("Origen o destino no encontrado.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // armamos la lista de adyacencia para el grafo
        List<List<DijkstraService.EdgeDto>> listaAdyacencia = armarListaAdyacencia(nodos, nombreAIndice);

        // inicializamos el mejor camino usando Dijkstra como punto de partida
        Candidate mejorCamino = inicializarMejorCaminoConDijkstra(idxOrigen, idxDestino, listaAdyacencia, nodos, alpha);

        // ahora exploramos con backtracking todos los caminos simples posibles
        explorarCaminosBacktracking(idxOrigen, idxDestino, listaAdyacencia, nodos, mejorCamino);

        if (Double.isInfinite(mejorCamino.totalCost)) {
            return new PathResponse("No se encontró camino.", Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        return new PathResponse(
                "Recorrido calculado exitosamente.",
                mejorCamino.nodeNames,
                mejorCamino.routeNames,
                mejorCamino.totalDistance,
                mejorCamino.totalCost
        );
    }

    // -------------------- métodos auxiliares --------------------

    // crea un mapa rápido para buscar el índice de un nodo por nombre
    private Map<String, Integer> armarMapaDeIndices(List<LocationDto> nodos) {
        Map<String, Integer> mapa = new HashMap<>();
        for (int i = 0; i < nodos.size(); i++) {
            String nombre = nodos.get(i).getNombre();
            if (nombre != null) mapa.put(nombre, i);
        }
        return mapa;
    }

    // crea la lista de adyacencia para el grafo
    private List<List<DijkstraService.EdgeDto>> armarListaAdyacencia(List<LocationDto> nodos, Map<String, Integer> nombreAIndice) {
        int n = nodos.size();
        List<List<DijkstraService.EdgeDto>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            LocationDto origen = nodos.get(i);
            List<RouteDto> rutas = origen.getRutas();
            if (rutas == null) continue;

            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer idxDestino = nombreAIndice.get(r.getDestino().getNombre());
                if (idxDestino != null) {
                    adj.get(i).add(new DijkstraService.EdgeDto(idxDestino, r.getDistancia(), r.getCosto(), r));
                }
            }
        }
        return adj;
    }

    // intenta inicializar el mejor camino usando Dijkstra (por costo y distancia)
    private Candidate inicializarMejorCaminoConDijkstra(int origen, int destino,
                                                        List<List<DijkstraService.EdgeDto>> adjList,
                                                        List<LocationDto> nodos,
                                                        double alpha) {
        Candidate mejor = new Candidate();

        // probamos con Dijkstra por costo
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

        // probamos con Dijkstra por distancia
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

    // backtracking: recorre caminos simples y actualiza mejor camino
    private void explorarCaminosBacktracking(int u, int target,
                                             List<List<DijkstraService.EdgeDto>> adjList,
                                             List<LocationDto> nodos,
                                             Candidate mejorCamino) {
        boolean[] visitados = new boolean[nodos.size()];
        List<String> caminoActual = new ArrayList<>();
        List<String> rutasActuales = new ArrayList<>();

        visitados[u] = true;
        caminoActual.add(nodos.get(u).getNombre());

        dfs(u, target, visitados, caminoActual, rutasActuales, 0.0, 0.0, adjList, nodos, mejorCamino);
    }

    // DFS recursivo con poda por costo
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

        if (u == target) {
            Candidate cand = new Candidate(new ArrayList<>(caminoActual), new ArrayList<>(rutasActuales), costoAcum, distanciaAcum);
            actualizarMejorCamino(cand, mejorCamino);
            return;
        }

        if (!Double.isInfinite(mejorCamino.totalCost) && costoAcum > mejorCamino.totalCost + EPS) return;

        for (DijkstraService.EdgeDto e : adjList.get(u)) {
            int v = e.to;
            if (visitados[v]) continue;

            RouteDto r = e.route;
            double d = (r == null) ? e.distance : r.getDistancia();
            double c = (r == null) ? e.cost : r.getCosto();
            String ruta = (r == null || r.getNombreRuta() == null) ? "?" : r.getNombreRuta();
            String nodo = (nodos.get(v).getNombre() == null) ? "?" : nodos.get(v).getNombre();

            if (!Double.isInfinite(mejorCamino.totalCost) && costoAcum + c > mejorCamino.totalCost + EPS) continue;

            // marcamos como visitado
            visitados[v] = true;
            rutasActuales.add(ruta);
            caminoActual.add(nodo);

            dfs(v, target, visitados, caminoActual, rutasActuales, distanciaAcum + d, costoAcum + c, adjList, nodos, mejorCamino);

            // desmarcamos para probar otras rutas
            visitados[v] = false;
            rutasActuales.remove(rutasActuales.size() - 1);
            caminoActual.remove(caminoActual.size() - 1);
        }
    }

    // si el candidato es mejor que el actual, lo guardamos
    private void actualizarMejorCamino(Candidate candidato, Candidate mejor) {
        if (Double.isInfinite(mejor.totalCost)) {
            mejor.copyFrom(candidato);
            return;
        }
        if (candidato.totalCost < mejor.totalCost - EPS) {
            mejor.copyFrom(candidato);
            return;
        }
        if (Math.abs(candidato.totalCost - mejor.totalCost) <= EPS && candidato.totalDistance < mejor.totalDistance - EPS) {
            mejor.copyFrom(candidato);
        }
    }

    // clase interna para guardar un candidato a mejor camino
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
