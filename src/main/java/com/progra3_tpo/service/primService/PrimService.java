package com.progra3_tpo.service.primService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Servicio que aplica el algoritmo de Prim "dirigido" a construir un árbol de expansión
 * y reconstruir un camino desde 'from' hasta 'to', priorizando aristas por una métrica dada.
 * Estructura clave: PriorityQueue (min-heap) por peso.
 * Tiempo (total): O(ElogV)
 */
@Service
public class PrimService {

    private final LocationRepository locationRepository;

    public PrimService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * computeOptimalPath
     * ------------------
     * ENTRA: nombres 'from', 'to', métrica ('distance' | 'cost' | 'combined') y alfa (si combined).
     * HACE: corre Prim desde 'from': va tomando la arista más barata hacia un nodo no visitado.
     *       Cuando alcanza 'to', corta y reconstruye el camino usando punteros 'padre'.
     * SALE: PathResponse con nodos/aristas del recorrido y totales de distancia/costo.
     */
    public PathResponse computeOptimalPath(String from, String to, String metric, double alpha) {
        // 1) Validaciones de existencia de nodos
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

        // 2) Estructuras auxiliares del algoritmo
        Set<String> visitados = new HashSet<>();
        PriorityQueue<EdgeCandidate> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(EdgeCandidate::getWeight)
        );
        Map<String, RouteDto> mejorArista = new HashMap<>(); // guarda la arista que conectó ese nodo
        Map<String, String> padre = new HashMap<>();         // puntero para reconstruir camino

        // 3) Inicialización: partimos desde 'from'
        visitados.add(from);
        agregarAristasVecinas(from, colaPrioridad, visitados, metric, alpha);

        // 4) Bucle principal de Prim: siempre tomo la arista mínima que sale del corte (visitados -> no visitados)
        while (!colaPrioridad.isEmpty()) {
            EdgeCandidate candidato = colaPrioridad.poll();
            String nodoDestino = candidato.getRuta().getDestino().getNombre();

            if (visitados.contains(nodoDestino)) {
                continue; // descartamos aristas cruzadas a nodos ya visitados
            }

            // Acepto esta arista como parte del "árbol" y muevo la frontera
            visitados.add(nodoDestino);
            mejorArista.put(nodoDestino, candidato.getRuta());
            padre.put(nodoDestino, candidato.getOrigenNombre());

            // Si ya alcancé el destino, puedo cortar (optimiza tiempo en grafos grandes)
            if (nodoDestino.equals(to)) {
                break;
            }

            // Expando la frontera con las aristas que salen del nuevo nodo agregado
            agregarAristasVecinas(nodoDestino, colaPrioridad, visitados, metric, alpha);
        }

        // 5) Reconstrucción del camino usando 'padre'
        return reconstruirCamino(from, to, mejorArista, padre);
    }

    /**
     * agregarAristasVecinas
     * ---------------------
     * ENTRA: nombre del nodo recién incorporado al corte, heap, conjunto visitados, métrica y alfa.
     * HACE: mira todas las rutas que salen de 'nodo'; por cada destino no visitado, calcula su peso
     *       (distance/cost/combined) y la encola en el heap como candidata.
     * SALE: heap con nuevas candidatas para el próximo paso de Prim.
     */
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

    /**
     * calcularPeso
     * ------------
     * ENTRA: la ruta (arista), la métrica y el parámetro alfa (si combined).
     * HACE: devuelve el peso de la arista según la política:
     *       - "distance"  → distancia
     *       - "cost"      → costo
     *       - "combined"  → alpha * distancia + (1 - alpha) * costo
     * SALE: double con el peso final.
     */
    private double calcularPeso(RouteDto ruta, String metric, double alpha) {
        return switch (metric.toLowerCase()) {
            case "distance" -> ruta.getDistancia();
            case "cost" -> ruta.getCosto();
            case "combined" -> alpha * ruta.getDistancia() + (1 - alpha) * ruta.getCosto();
            default -> ruta.getDistancia();
        };
    }

    /**
     * reconstruirCamino
     * -----------------
     * ENTRA: 'from', 'to', mapas 'mejorArista' (qué arista introdujo cada nodo) y 'padre' (quién lo conectó).
     * HACE: camina desde 'to' hacia atrás con 'padre' hasta llegar a 'from', acumulando nodos/aristas
     *       y sumando distancia/costo.
     * SALE: PathResponse con el recorrido en orden correcto (from → ... → to).
     */
    private PathResponse reconstruirCamino(String from, String to,
                                           Map<String, RouteDto> mejorArista,
                                           Map<String, String> padre) {
        List<String> nodosRecorrido = new ArrayList<>();
        List<String> aristasRecorrido = new ArrayList<>();
        double distanciaTotal = 0.0;
        double costoTotal = 0.0;

        String nodoActual = to;

        // Bajamos desde 'to' hasta 'from' usando los punteros 'padre'
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

            // Insertamos al principio para mantener orden from→to
            nodosRecorrido.add(0, nodoActual);
            aristasRecorrido.add(0, arista.getNombreRuta());
            distanciaTotal += arista.getDistancia();
            costoTotal += arista.getCosto();

            nodoActual = padre.get(nodoActual);
        }

        // Agrega el origen al inicio
        nodosRecorrido.add(0, from);

        return new PathResponse(
                "Recorrido calculado exitosamente usando algoritmo de Prim",
                nodosRecorrido,
                aristasRecorrido,
                distanciaTotal,
                costoTotal
        );
    }

    // ------------------------------------------------------------
    // Clase interna: representa una arista candidata con su peso.
    // Se guarda también el nombre del nodo origen para armar 'padre'.
    // ------------------------------------------------------------
    private static class EdgeCandidate {
        private final RouteDto ruta;
        private final double weight;
        private final String origenNombre;

        public EdgeCandidate(RouteDto ruta, double weight, String origenNombre) {
            this.ruta = ruta;
            this.weight = weight;
            this.origenNombre = origenNombre;
        }

        public RouteDto getRuta() { return ruta; }
        public double getWeight() { return weight; }
        public String getOrigenNombre() { return origenNombre; }
    }
}