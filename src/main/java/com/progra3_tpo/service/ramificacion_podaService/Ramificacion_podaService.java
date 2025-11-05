package com.progra3_tpo.service.ramificacion_podaService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Ramificacion_podaService
 * -------------------------------------------------------------
 * Algoritmo: Branch & Bound (Ramificación y Poda) sobre un grafo.
 * Estrategia: explora caminos en una cola de prioridad ordenada por cota inferior (heurística).
 *   - "Ramificación": generar hijos (expandir vecinos).
 *   - "Poda": descartar nodos cuyo bound >= mejorCota.
 * Métrica: distance | cost | combined (alpha*dist + (1-alpha)*cost).
 * COSTO : T=O(Nlog N) donde N es la cantidad de nodos-estado generados y procesados (expandidos + encolados).
 */
@Service
public class Ramificacion_podaService {

    private final LocationRepository locationRepository;
    private PathResponse mejorSolucion;
    private double mejorCota;

    public Ramificacion_podaService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * computeOptimalPath
     * ------------------
     * ENTRA: from, to, metric, alpha.
     * HACE:
     *   1) Valida nodos.
     *   2) Inicializa mejorCota = +∞ y PQ ordenada por cotaInferior.
     *   3) Encola nodo inicial con cota.
     *   4) Mientras haya nodos en PQ:
     *        - Saca el de menor cota (poll).
     *        - Poda si cota >= mejorCota.
     *        - Si llegó a 'to': evalúa costo real y actualiza mejor solución.
     *        - Si no: ramifica (genera hijos) y encola los prometedores (bound < mejorCota).
     *   5) Reconstruye/retorna mejor solución (o “no hay camino”).
     * SALE: PathResponse (nodos, aristas, distancia y costo).
     */
    public PathResponse computeOptimalPath(String from, String to, String metric, double alpha) {
        // Verificar que existan los nodos
        LocationDto origenLocation = locationRepository.findByNombre(from);
        LocationDto destinoLocation = locationRepository.findByNombre(to);

        if (origenLocation == null || destinoLocation == null) {
            return new PathResponse(
                    "Nodo de origen o destino no existe en el grafo",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0,
                    0.0
            );
        }

        // Inicializar variables para Branch and Bound
        mejorSolucion = null;
        mejorCota = Double.MAX_VALUE;

        // Cola de prioridad para explorar nodos (ordenada por cota inferior)
        PriorityQueue<Nodo> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(Nodo::getCotaInferior)
        );

        // Crear nodo inicial
        List<String> caminoInicial = new ArrayList<>();
        caminoInicial.add(from);
        List<String> aristasInicial = new ArrayList<>();
        Nodo nodoInicial = new Nodo(from, caminoInicial, aristasInicial, 0.0, 0.0);
        nodoInicial.calcularCotaInferior(to, metric, alpha);

        colaPrioridad.add(nodoInicial);

        // Algoritmo de Ramificación y Poda (Branch and Bound)
        while (!colaPrioridad.isEmpty()) {
            Nodo nodoActual = colaPrioridad.poll();

            // Poda: si la cota inferior es ≥ mejorCota, descartar
            if (nodoActual.getCotaInferior() >= mejorCota) {
                continue;
            }

            String ultimoNodo = nodoActual.getUltimoNodo();

            // Si llegamos al destino, actualizar la mejor solución
            if (ultimoNodo.equals(to)) {
                double pesoTotal = calcularPesoTotal(nodoActual, metric, alpha);
                if (pesoTotal < mejorCota) {
                    mejorCota = pesoTotal;
                    mejorSolucion = construirRespuesta(nodoActual);
                }
                continue;
            }

            // Expandir vecinos (ramificación)
            LocationDto location = locationRepository.findByNombre(ultimoNodo);
            if (location != null && location.getRutas() != null) {
                for (RouteDto arista : location.getRutas()) {
                    String destino = arista.getDestino().getNombre();

                    // No visitar nodos ya visitados (evitar ciclos)
                    if (!nodoActual.getCamino().contains(destino)) {
                        // Crear nuevo nodo hijo
                        List<String> nuevoCamino = new ArrayList<>(nodoActual.getCamino());
                        nuevoCamino.add(destino);

                        List<String> nuevasAristas = new ArrayList<>(nodoActual.getAristas());
                        nuevasAristas.add(arista.getNombreRuta());

                        double nuevaDistancia = nodoActual.getDistanciaAcumulada() + arista.getDistancia();
                        double nuevoCosto = nodoActual.getCostoAcumulado() + arista.getCosto();

                        Nodo nuevoNodo = new Nodo(destino, nuevoCamino, nuevasAristas,
                                nuevaDistancia, nuevoCosto);
                        nuevoNodo.calcularCotaInferior(to, metric, alpha);

                        // Solo agregar si la cota inferior es prometedora
                        if (nuevoNodo.getCotaInferior() < mejorCota) {
                            colaPrioridad.add(nuevoNodo);
                        }
                    }
                }
            }
        }

        // Retornar la mejor solución encontrada o indicar que no hay camino
        if (mejorSolucion == null) {
            return new PathResponse(
                    "No se encontró un camino entre " + from + " y " + to,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0,
                    0.0
            );
        }

        return mejorSolucion;
    }

    /**
     * calcularPesoTotal
     * -----------------
     * ENTRA: un Nodo (estado parcial) y la política de métrica.
     * HACE: devuelve el valor de la función objetivo del estado (para comparar soluciones).
     * SALE: double (distance | cost | combined).
     */
    private double calcularPesoTotal(Nodo nodo, String metric, double alpha) {
        return switch (metric.toLowerCase()) {
            case "distance" -> nodo.getDistanciaAcumulada();
            case "cost" -> nodo.getCostoAcumulado();
            case "combined" -> alpha * nodo.getDistanciaAcumulada()
                    + (1 - alpha) * nodo.getCostoAcumulado();
            default -> nodo.getDistanciaAcumulada();
        };
    }

    /**
     * construirRespuesta
     * ------------------
     * ENTRA: Nodo hoja (llegó a 'to'), con su camino y aristas acumuladas.
     * HACE: arma el PathResponse con los datos recolectados.
     * SALE: PathResponse listo para el controller.
     */
    private PathResponse construirRespuesta(Nodo nodo) {
        return new PathResponse(
                "Recorrido calculado exitosamente usando algoritmo de Ramificación y Poda",
                new ArrayList<>(nodo.getCamino()),
                new ArrayList<>(nodo.getAristas()),
                nodo.getDistanciaAcumulada(),
                nodo.getCostoAcumulado()
        );
    }

    // -------------------------------------------------------------------------
    // Clase interna: Nodo de búsqueda para Branch & Bound
    // Guarda el último vértice, el camino y aristas recorridas, acumulados y cota.
    // -------------------------------------------------------------------------
    private static class Nodo {
        private final String ultimoNodo;
        private final List<String> camino;
        private final List<String> aristas;
        private final double distanciaAcumulada;
        private final double costoAcumulado;
        private double cotaInferior;

        public Nodo(String ultimoNodo, List<String> camino, List<String> aristas,
                    double distanciaAcumulada, double costoAcumulado) {
            this.ultimoNodo = ultimoNodo;
            this.camino = camino;
            this.aristas = aristas;
            this.distanciaAcumulada = distanciaAcumulada;
            this.costoAcumulado = costoAcumulado;
            this.cotaInferior = 0.0;
        }

        /**
         * calcularCotaInferior
         * --------------------
         * ENTRA: destino (no usado en esta cota trivial), metric, alpha.
         * HACE: define la cota (bound) = peso acumulado (heurística optimista y admisible si el resto ≥ 0).
         *       SUGERENCIA: se puede mejorar sumando una heurística consistente (e.g., una relajación tipo
         *       "mínimo edge saliente" o una distancia euclidiana si hubiera coordenadas).
         * SALE: fija 'cotaInferior' en el nodo.
         */
        public void calcularCotaInferior(String destino, String metric, double alpha) {
            double pesoAcumulado = switch (metric.toLowerCase()) {
                case "distance" -> distanciaAcumulada;
                case "cost" -> costoAcumulado;
                case "combined" -> alpha * distanciaAcumulada
                        + (1 - alpha) * costoAcumulado;
                default -> distanciaAcumulada;
            };

            // Heurística simple (admisible): solo acumulado (asume resto ≥ 0)
            this.cotaInferior = pesoAcumulado;
        }

        public String getUltimoNodo() { return ultimoNodo; }
        public List<String> getCamino() { return camino; }
        public List<String> getAristas() { return aristas; }
        public double getDistanciaAcumulada() { return distanciaAcumulada; }
        public double getCostoAcumulado() { return costoAcumulado; }
        public double getCotaInferior() { return cotaInferior; }
    }
}