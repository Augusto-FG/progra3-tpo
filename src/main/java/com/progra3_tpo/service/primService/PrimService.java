package com.progra3_tpo.service.primService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PrimService {

    private final LocationRepository locationRepository;

    public PrimService(LocationRepository locationRepository) {
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

        Set<String> visitados = new HashSet<>();
        PriorityQueue<EdgeCandidate> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(EdgeCandidate::getWeight)
        );

        Map<String, RouteDto> mejorArista = new HashMap<>();
        Map<String, String> padre = new HashMap<>();

        // Inicializar desde el nodo origen
        visitados.add(from);
        agregarAristasVecinas(from, colaPrioridad, visitados, metric, alpha);

        // Algoritmo de Prim
        while (!colaPrioridad.isEmpty()) {
            EdgeCandidate candidato = colaPrioridad.poll();
            String nodoDestino = candidato.getRuta().getDestino().getNombre();

            if (visitados.contains(nodoDestino)) {
                continue;
            }

            visitados.add(nodoDestino);
            mejorArista.put(nodoDestino, candidato.getRuta());
            padre.put(nodoDestino, candidato.getOrigenNombre());

            // Si llegamos al destino, terminamos
            if (nodoDestino.equals(to)) {
                break;
            }

            agregarAristasVecinas(nodoDestino, colaPrioridad, visitados, metric, alpha);
        }

        // Reconstruir el camino desde destino hasta origen
        return reconstruirCamino(from, to, mejorArista, padre);
    }

    private void agregarAristasVecinas(String nodo, PriorityQueue<EdgeCandidate> cola,
                                       Set<String> visitados, String metric, double alpha) {
        LocationDto location = locationRepository.findByNombre(nodo);
        if (location != null && location.getRutas() != null) {
            for (RouteDto ruta : location.getRutas()) {
                String destino = ruta.getDestino().getNombre();
                if (!visitados.contains(destino)) {
                    double peso = calcularPeso(ruta, metric, alpha);
                    cola.add(new EdgeCandidate(ruta, peso, nodo));
                }
            }
        }
    }

    private double calcularPeso(RouteDto ruta, String metric, double alpha) {
        return switch (metric.toLowerCase()) {
            case "distance" -> ruta.getDistancia();
            case "cost" -> ruta.getCosto();
            case "combined" -> alpha * ruta.getDistancia() + (1 - alpha) * ruta.getCosto();
            default -> ruta.getDistancia();
        };
    }

    private PathResponse reconstruirCamino(String from, String to,
                                           Map<String, RouteDto> mejorArista,
                                           Map<String, String> padre) {
        List<String> nodosRecorrido = new ArrayList<>();
        List<String> aristasRecorrido = new ArrayList<>();
        double distanciaTotal = 0.0;
        double costoTotal = 0.0;

        String nodoActual = to;

        // Reconstruir el camino desde el destino hasta el origen
        while (nodoActual != null && !nodoActual.equals(from)) {
            RouteDto arista = mejorArista.get(nodoActual);
            if (arista == null) {
                return new PathResponse(
                        "No se encontró un camino entre " + from + " y " + to,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0.0,
                        0.0
                );
            }

            nodosRecorrido.add(0, nodoActual);
            aristasRecorrido.add(0, arista.getNombreRuta());
            distanciaTotal += arista.getDistancia();
            costoTotal += arista.getCosto();

            nodoActual = padre.get(nodoActual);
        }

        // Agregar el nodo origen al principio
        nodosRecorrido.add(0, from);

        return new PathResponse(
                "Recorrido calculado exitosamente usando algoritmo de Prim",
                nodosRecorrido,
                aristasRecorrido,
                distanciaTotal,
                costoTotal
        );
    }

    // Clase interna para manejar candidatos de aristas con su peso
    private static class EdgeCandidate {
        private final RouteDto ruta;
        private final double weight;
        private final String origenNombre;

        public EdgeCandidate(RouteDto ruta, double weight, String origenNombre) {
            this.ruta = ruta;
            this.weight = weight;
            this.origenNombre = origenNombre;
        }

        public RouteDto getRuta() {
            return ruta;
        }

        public double getWeight() {
            return weight;
        }

        public String getOrigenNombre() {
            return origenNombre;
        }
    }
}
