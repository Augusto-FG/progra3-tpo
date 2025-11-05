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
    private static final double EPS = 1e-9; // Margen de error para comparar valores en coma flotante

    public DfsService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * Método principal que calcula un recorrido usando DFS puro (sin heurísticas ni pesos intermedios).
     * Busca un camino desde un nodo origen (from) hasta un destino (to).
     */
    public PathResponse computeDfsPure(String from, String to) {

        // 1. Validación de parámetros
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return new PathResponse("Datos inválidos: se requiere 'from' y 'to'.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // 2. Traemos todos los nodos disponibles en la base de datos
        List<LocationDto> nodos = locationRepository.findAll();
        if (nodos == null || nodos.isEmpty()) {
            return new PathResponse("No hay nodos cargados en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // 3. Asociamos cada nombre de nodo con un índice numérico
        Map<String, Integer> nombreAIndice = new HashMap<>();
        for (int i = 0; i < nodos.size(); i++) {
            String nombre = nodos.get(i).getNombre();
            if (nombre != null) nombreAIndice.put(nombre, i);
        }

        // 4. Verificamos que existan los nodos de origen y destino
        Integer indiceOrigen = nombreAIndice.get(from);
        Integer indiceDestino = nombreAIndice.get(to);
        if (indiceOrigen == null || indiceDestino == null) {
            return new PathResponse("No se encontró el origen o destino en la base de datos.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        // 5. Construimos la lista de adyacencia (grafo)
        List<List<DijkstraService.EdgeDto>> grafo = construirListaAdyacencia(nodos, nombreAIndice);

        // 6. Preparamos estructuras auxiliares para el recorrido DFS
        boolean[] visitado = new boolean[nodos.size()];
        List<String> recorridoActual = new ArrayList<>();
        List<String> rutasActuales = new ArrayList<>();
        Candidate mejorCamino = new Candidate();

        // Marcamos el nodo inicial
        visitado[indiceOrigen] = true;
        recorridoActual.add(nodos.get(indiceOrigen).getNombre());

        // 7. Ejecutamos DFS recursivo
        buscarDFS(indiceOrigen, indiceDestino, visitado, recorridoActual, rutasActuales,
                0.0, 0.0, grafo, nodos, mejorCamino);

        // 8. Retornamos la respuesta según si se encontró o no un camino
        if (Double.isInfinite(mejorCamino.totalCost)) {
            return new PathResponse("No existe un recorrido entre el origen y el destino.",
                    Collections.emptyList(), Collections.emptyList(), 0.0, 0.0);
        }

        return new PathResponse("Recorrido encontrado exitosamente.",
                mejorCamino.nodeNames, mejorCamino.routeNames, mejorCamino.totalDistance, mejorCamino.totalCost);
    }

    /**
     * Construye la lista de adyacencia del grafo a partir de los nodos y sus rutas.
     */
    private List<List<DijkstraService.EdgeDto>> construirListaAdyacencia(List<LocationDto> nodos,
                                                                         Map<String, Integer> nombreAIndice) {

        int n = nodos.size();
        List<List<DijkstraService.EdgeDto>> grafo = new ArrayList<>(n);
        for (int i = 0; i < n; i++) grafo.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            LocationDto origen = nodos.get(i);
            List<RouteDto> rutas = origen.getRutas();
            if (rutas == null) continue;

            for (RouteDto ruta : rutas) {
                if (ruta == null || ruta.getDestino() == null || ruta.getDestino().getNombre() == null) continue;
                Integer destinoIdx = nombreAIndice.get(ruta.getDestino().getNombre());
                if (destinoIdx == null) continue;

                // Agregamos la conexión entre origen y destino
                grafo.get(i).add(new DijkstraService.EdgeDto(destinoIdx, ruta.getDistancia(), ruta.getCosto(), ruta));
            }
        }
        return grafo;
    }

    /**
     * DFS recursivo: explora todos los caminos posibles desde el nodo actual hasta el destino.
     */
    private void buscarDFS(int actual,
                           int destino,
                           boolean[] visitado,
                           List<String> nodosActuales,
                           List<String> rutasActuales,
                           double distanciaActual,
                           double costoActual,
                           List<List<DijkstraService.EdgeDto>> grafo,
                           List<LocationDto> nodos,
                           Candidate mejorCamino) {

        // Caso base: si llegamos al destino, comparamos con el mejor camino encontrado
        if (actual == destino) {
            Candidate candidato = new Candidate(new ArrayList<>(nodosActuales), new ArrayList<>(rutasActuales),
                    costoActual, distanciaActual);
            actualizarSiEsMejor(candidato, mejorCamino);
            return;
        }

        // Si el costo actual ya es peor que el mejor encontrado, cortamos la rama (poda)
        if (!Double.isInfinite(mejorCamino.totalCost) && costoActual > mejorCamino.totalCost + EPS) return;

        List<DijkstraService.EdgeDto> conexiones = grafo.get(actual);
        if (conexiones == null || conexiones.isEmpty()) return;

        // Recorremos los vecinos del nodo actual
        for (DijkstraService.EdgeDto arista : conexiones) {
            int vecino = arista.to;
            if (visitado[vecino]) continue; // evitamos ciclos

            RouteDto ruta = arista.route;
            double distancia = (ruta == null) ? arista.distance : ruta.getDistancia();
            double costo = (ruta == null) ? arista.cost : ruta.getCosto();
            String nombreRuta = (ruta == null || ruta.getNombreRuta() == null) ? "?" : ruta.getNombreRuta();
            String nombreNodo = (nodos.get(vecino).getNombre() == null) ? "?" : nodos.get(vecino).getNombre();

            // Poda adicional por costo acumulado
            if (!Double.isInfinite(mejorCamino.totalCost) && costoActual + costo > mejorCamino.totalCost + EPS) continue;

            // Avanzamos
            visitado[vecino] = true;
            rutasActuales.add(nombreRuta);
            nodosActuales.add(nombreNodo);

            // Llamada recursiva
            buscarDFS(vecino, destino, visitado, nodosActuales, rutasActuales,
                    distanciaActual + distancia, costoActual + costo, grafo, nodos, mejorCamino);

            // Retroceso (backtracking): desmarcamos el nodo y eliminamos los últimos elementos
            visitado[vecino] = false;
            rutasActuales.remove(rutasActuales.size() - 1);
            nodosActuales.remove(nodosActuales.size() - 1);
        }
    }

    /**
     * Si el nuevo camino es mejor que el mejor actual, lo reemplaza.
     */
    private void actualizarSiEsMejor(Candidate candidato, Candidate mejor) {
        if (Double.isInfinite(mejor.totalCost)) {
            mejor.copyFrom(candidato);
            return;
        }
        if (candidato.totalCost < mejor.totalCost - EPS) {
            mejor.copyFrom(candidato);
            return;
        }
        if (Math.abs(candidato.totalCost - mejor.totalCost) <= EPS &&
                candidato.totalDistance < mejor.totalDistance - EPS) {
            mejor.copyFrom(candidato);
        }
    }

    /**
     * Clase auxiliar para guardar el mejor camino encontrado hasta el momento.
     */
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
