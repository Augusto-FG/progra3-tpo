package com.progra3_tpo.service.kruscalService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * KruscalService
 * -------------------------------------------------------------
 * Algoritmo: Kruskal para construir un Árbol/Foresta de Expansión Mínima (MST)
 * sobre el grafo ponderado según una métrica.
 * Luego busca el camino en ese MST entre 'from' y 'to' con BFS.
 **/
@Service
public class KruscalService {

    private final LocationRepository locationRepository;

    public KruscalService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * computeOptimalPath
     * ------------------
     * ENTRA: from, to, metric, alpha.
     * HACE:
     *   1) Valida nodos.
     *   2) Junta todas las aristas y las ordena por peso (según metric/alpha).
     *   3) Recorre aristas de menor a mayor; usa Union-Find para evitar ciclos y formar el MST.
     *   4) Construye un grafo (adjacency list) del MST.
     *   5) Ejecuta BFS en el MST para obtener el camino from→to.
     * SALE: PathResponse con nodos/aristas y totales.
     * COMPLEJIDAD: O(E log E) dominante por el sort.
     */
    public PathResponse computeOptimalPath(String from, String to, String metric, double alpha) {
        // Obtener nodos
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

        // Obtener todas las aristas y ordenarlas por peso
        List<RouteDto> todasLasAristas = obtenerTodasLasAristas();
        todasLasAristas.sort(Comparator.comparingDouble(r ->
                calcularPeso(r, metric, alpha)));

        // Union-Find para detectar ciclos
        UnionFind uf = new UnionFind();
        List<RouteDto> aristasArbolExpansion = new ArrayList<>();

        // Algoritmo de Kruskal - construir MST
        for (RouteDto arista : todasLasAristas) {
            LocationDto origen = obtenerOrigenDeArista(arista);
            LocationDto destino = arista.getDestino();

            if (origen != null && destino != null) {
                if (!uf.connected(origen.getNombre(), destino.getNombre())) {
                    uf.union(origen.getNombre(), destino.getNombre());
                    aristasArbolExpansion.add(arista);
                }
            }
        }

        // Construir grafo del MST
        Map<String, List<RouteDto>> mst = construirGrafoMST(aristasArbolExpansion);

        // Buscar camino en el MST desde origen a destino usando BFS
        return encontrarCaminoEnMST(from, to, mst);
    }

    /**
     * obtenerTodasLasAristas
     * ----------------------
     * ENTRA: nada.
     * HACE: recorre todos los LocationDto y acumula sus rutas, evitando duplicados
     *       (para grafos no dirigidos) con un set de pares (u-v) y (v-u).
     * SALE: lista con todas las aristas únicas.
     */
    private List<RouteDto> obtenerTodasLasAristas() {
        List<RouteDto> aristas = new ArrayList<>();
        Set<String> procesados = new HashSet<>();

        Iterable<LocationDto> todasLocations = locationRepository.findAll();

        for (LocationDto location : todasLocations) {
            if (location.getRutas() != null) {
                for (RouteDto ruta : location.getRutas()) {
                    String key = location.getNombre() + "-" + ruta.getDestino().getNombre();
                    String keyInverso = ruta.getDestino().getNombre() + "-" + location.getNombre();

                    if (!procesados.contains(key) && !procesados.contains(keyInverso)) {
                        aristas.add(ruta);
                        procesados.add(key);
                    }
                }
            }
        }
        return aristas;
    }

    /**
     * obtenerOrigenDeArista
     * ---------------------
     * ENTRA: una arista (RouteDto).
     * HACE: escanea las locations y devuelve aquella que contiene a esa arista en su lista de rutas.
     * SALE: el LocationDto origen o null.
     * COMPLEJIDAD: O(V + grado(origen)). Costoso, ver notas de optimización.
     */
    private LocationDto obtenerOrigenDeArista(RouteDto arista) {
        Iterable<LocationDto> todasLocations = locationRepository.findAll();
        for (LocationDto location : todasLocations) {
            if (location.getRutas() != null && location.getRutas().contains(arista)) {
                return location;
            }
        }
        return null;
    }

    /**
     * construirGrafoMST
     * -----------------
     * ENTRA: lista de aristas seleccionadas por Kruskal (MST).
     * HACE: arma una lista de adyacencia bidireccional (clona arista inversa).
     * SALE: Map<String, List<RouteDto>> (adjacency list del MST).
     * COMPLEJIDAD: O(E_MST) ≈ O(V).
     */
    private Map<String, List<RouteDto>> construirGrafoMST(List<RouteDto> aristas) {
        Map<String, List<RouteDto>> mst = new HashMap<>();

        for (RouteDto arista : aristas) {
            LocationDto origen = obtenerOrigenDeArista(arista);
            LocationDto destino = arista.getDestino();

            if (origen != null && destino != null) {
                mst.computeIfAbsent(origen.getNombre(), k -> new ArrayList<>())
                        .add(arista);

                // Agregar arista bidireccional (crear copia invertida)
                RouteDto aristaInversa = new RouteDto(
                        arista.getNombreRuta(),
                        arista.getDistancia(),
                        arista.getCosto(),
                        arista.getTipoCamino(),
                        origen
                );
                mst.computeIfAbsent(destino.getNombre(), k -> new ArrayList<>())
                        .add(aristaInversa);
            }
        }
        return mst;
    }

    /**
     * encontrarCaminoEnMST (BFS)
     * --------------------------
     * ENTRA: from, to y el MST como adjacency list.
     * HACE: BFS para hallar el camino simple (en un árbol hay único camino).
     * SALE: PathResponse con camino y totales, o “no hay camino”.
     * COMPLEJIDAD: O(V + E_MST) ≈ O(V).
     */
    private PathResponse encontrarCaminoEnMST(String from, String to, Map<String, List<RouteDto>> mst) {
        Queue<String> cola = new LinkedList<>();
        Map<String, RouteDto> aristaPrevia = new HashMap<>();
        Set<String> visitados = new HashSet<>();

        cola.add(from);
        visitados.add(from);

        // BFS para encontrar el camino
        while (!cola.isEmpty()) {
            String nodoActual = cola.poll();

            if (nodoActual.equals(to)) {
                break;
            }

            List<RouteDto> vecinos = mst.get(nodoActual);
            if (vecinos != null) {
                for (RouteDto arista : vecinos) {
                    String destino = arista.getDestino().getNombre();
                    if (!visitados.contains(destino)) {
                        visitados.add(destino);
                        aristaPrevia.put(destino, arista);
                        cola.add(destino);
                    }
                }
            }
        }

        // Reconstruir el camino
        return reconstruirCamino(from, to, aristaPrevia);
    }

    /**
     * reconstruirCamino
     * -----------------
     * ENTRA: from, to y mapa aristaPrevia (para cada nodo, la arista con la que llegué).
     * HACE: camina hacia atrás desde 'to' usando aristaPrevia y obtiene nodos/aristas.
     * SALE: PathResponse listo (o mensaje de no camino).
     * COMPLEJIDAD: O(L) (L = longitud del camino).
     */
    private PathResponse reconstruirCamino(String from, String to, Map<String, RouteDto> aristaPrevia) {
        if (!aristaPrevia.containsKey(to) && !from.equals(to)) {
            return new PathResponse(
                    "No se encontró un camino entre " + from + " y " + to,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0,
                    0.0
            );
        }

        List<String> nodosRecorrido = new ArrayList<>();
        List<String> aristasRecorrido = new ArrayList<>();
        double distanciaTotal = 0.0;
        double costoTotal = 0.0;

        String nodoActual = to;

        // Reconstruir el camino desde el destino hasta el origen
        while (!nodoActual.equals(from)) {
            RouteDto arista = aristaPrevia.get(nodoActual);
            nodosRecorrido.add(0, nodoActual);
            aristasRecorrido.add(0, arista.getNombreRuta());
            distanciaTotal += arista.getDistancia();
            costoTotal += arista.getCosto();

            // Obtener el origen de la arista actual
            LocationDto origenArista = obtenerOrigenDeArista(arista);
            if (origenArista != null) {
                nodoActual = origenArista.getNombre();
            } else {
                break;
            }
        }

        nodosRecorrido.add(0, from);

        return new PathResponse(
                "Recorrido calculado exitosamente usando algoritmo de Kruskal",
                nodosRecorrido,
                aristasRecorrido,
                distanciaTotal,
                costoTotal
        );
    }

    /**
     * calcularPeso
     * ------------
     * ENTRA: una ruta y la política (metric/alpha).
     * HACE: devuelve el peso de la arista.
     * SALE: double.
     * COMPLEJIDAD: O(1).
     */
    private double calcularPeso(RouteDto ruta, String metric, double alpha) {
        return switch (metric.toLowerCase()) {
            case "distance" -> ruta.getDistancia();
            case "cost" -> ruta.getCosto();
            case "combined" -> alpha * ruta.getDistancia()
                    + (1 - alpha) * ruta.getCosto();
            default -> ruta.getDistancia();
        };
    }

    private static class UnionFind {
        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Integer> rank = new HashMap<>();

        public String find(String node) {
            if (!parent.containsKey(node)) {
                parent.put(node, node);
                rank.put(node, 0);
            }
            if (!parent.get(node).equals(node)) {
                parent.put(node, find(parent.get(node)));
            }
            return parent.get(node);
        }

        public void union(String node1, String node2) {
            String root1 = find(node1);
            String root2 = find(node2);

            if (!root1.equals(root2)) {
                int rank1 = rank.get(root1);
                int rank2 = rank.get(root2);

                if (rank1 > rank2) {
                    parent.put(root2, root1);
                } else if (rank1 < rank2) {
                    parent.put(root1, root2);
                } else {
                    parent.put(root2, root1);
                    rank.put(root1, rank1 + 1);
                }
            }
        }

        public boolean connected(String node1, String node2) {
            return find(node1).equals(find(node2));
        }
    }
}