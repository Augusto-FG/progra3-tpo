package com.progra3_tpo.service.ramificacion_podaService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Ramificacion_podaService {

    private final LocationRepository locationRepository;
    private PathResponse mejorSolucion;
    private double mejorCota;

    public Ramificacion_podaService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

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

            // Poda: si la cota inferior es mayor o igual a la mejor solución encontrada, descartar
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

            // Expandir vecinos
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

                        Nodo nuevoNodo = new Nodo(destino, nuevoCamino, nuevasAristas, nuevaDistancia, nuevoCosto);
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

    private double calcularPesoTotal(Nodo nodo, String metric, double alpha) {
        return switch (metric.toLowerCase()) {
            case "distance" -> nodo.getDistanciaAcumulada();
            case "cost" -> nodo.getCostoAcumulado();
            case "combined" -> alpha * nodo.getDistanciaAcumulada() + (1 - alpha) * nodo.getCostoAcumulado();
            default -> nodo.getDistanciaAcumulada();
        };
    }

    private PathResponse construirRespuesta(Nodo nodo) {
        return new PathResponse(
                "Recorrido calculado exitosamente usando algoritmo de Ramificación y Poda",
                new ArrayList<>(nodo.getCamino()),
                new ArrayList<>(nodo.getAristas()),
                nodo.getDistanciaAcumulada(),
                nodo.getCostoAcumulado()
        );
    }

    // Clase interna para representar un nodo en el árbol de búsqueda
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

        public void calcularCotaInferior(String destino, String metric, double alpha) {
            // Heurística: costo acumulado + estimación optimista del costo restante
            // La estimación puede ser 0 (cota trivial) o usar una heurística
            double pesoAcumulado = switch (metric.toLowerCase()) {
                case "distance" -> distanciaAcumulada;
                case "cost" -> costoAcumulado;
                case "combined" -> alpha * distanciaAcumulada + (1 - alpha) * costoAcumulado;
                default -> distanciaAcumulada;
            };

            // Heurística simple: peso acumulado (puede mejorarse con distancia en línea recta, etc.)
            this.cotaInferior = pesoAcumulado;
        }

        public String getUltimoNodo() {
            return ultimoNodo;
        }

        public List<String> getCamino() {
            return camino;
        }

        public List<String> getAristas() {
            return aristas;
        }

        public double getDistanciaAcumulada() {
            return distanciaAcumulada;
        }

        public double getCostoAcumulado() {
            return costoAcumulado;
        }

        public double getCotaInferior() {
            return cotaInferior;
        }
    }
}
