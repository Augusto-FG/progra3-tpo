package com.progra3_tpo.service.grafoService;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio de grafo con Dijkstra (camino mínimo por peso).
 * Nota: las aristas y sus pesos son de ejemplo. Para usar datos reales, construir el grafo desde Neo4j.
 */
@Service
public class GrafoService {

    private final int V = 6; // cantidad de nodos
    private final LinkedList<Edge>[] adj; // adyacencias ponderadas

    public GrafoService() {
        adj = new LinkedList[V];
        for (int i = 0; i < V; i++) adj[i] = new LinkedList<>();

        // Ejemplo simple de red de rutas con pesos (distancias)
        agregarArista(0, 1, 5.0); // depósito ↔ cliente A
        agregarArista(0, 2, 8.0); // depósito ↔ cliente B
        agregarArista(1, 3, 4.0);
        agregarArista(2, 4, 6.5);
        agregarArista(3, 5, 3.0);
    }

    private static class Edge {
        final int to;
        final double weight;
        Edge(int to, double weight) { this.to = to; this.weight = weight; }
    }

    public void agregarArista(int v, int w, double weight) {
        if (v < 0 || v >= V || w < 0 || w >= V) return;
        adj[v].add(new Edge(w, weight));
        adj[w].add(new Edge(v, weight)); // grafo no dirigido
    }

    public List<Integer> bfs(int inicio) {
        List<Integer> recorrido = new ArrayList<>();
        if (!enRango(inicio)) return recorrido;
        boolean[] visitado = new boolean[V];
        Queue<Integer> cola = new LinkedList<>();
        visitado[inicio] = true;
        cola.add(inicio);

        while (!cola.isEmpty()) {
            int nodo = cola.poll();
            recorrido.add(nodo);
            for (Edge e : adj[nodo]) {
                if (!visitado[e.to]) {
                    visitado[e.to] = true;
                    cola.add(e.to);
                }
            }
        }
        return recorrido;
    }

    public List<Integer> dfs(int inicio) {
        List<Integer> recorrido = new ArrayList<>();
        if (!enRango(inicio)) return recorrido;
        boolean[] visitado = new boolean[V];
        dfsRec(inicio, visitado, recorrido);
        return recorrido;
    }

    private void dfsRec(int v, boolean[] visitado, List<Integer> recorrido) {
        visitado[v] = true;
        recorrido.add(v);
        for (Edge e : adj[v]) {
            if (!visitado[e.to]) dfsRec(e.to, visitado, recorrido);
        }
    }

    private boolean enRango(int nodo) {
        return nodo >= 0 && nodo < V;
    }

    /**
     * Dijkstra: devuelve la lista de nodos desde inicio hasta fin (incluyendo ambos)
     * o lista vacía si no existe camino o los índices no son válidos.
     */
    public List<Integer> dijkstra(int inicio, int fin) {
        if (!enRango(inicio) || !enRango(fin)) return Collections.emptyList();
        if (inicio == fin) return List.of(inicio);

        double[] dist = new double[V];
        int[] prev = new int[V];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[inicio] = 0.0;

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingDouble(i -> dist[i]));
        pq.add(inicio);

        boolean[] visited = new boolean[V];

        while (!pq.isEmpty()) {
            int u = pq.poll();
            if (visited[u]) continue;
            visited[u] = true;
            if (u == fin) break;

            for (Edge e : adj[u]) {
                int v = e.to;
                double alt = dist[u] + e.weight;
                if (alt < dist[v]) {
                    dist[v] = alt;
                    prev[v] = u;
                    pq.add(v);
                }
            }
        }

        if (prev[fin] == -1 && fin != inicio) {
            // no se alcanzó fin
            return Collections.emptyList();
        }

        // reconstruir camino
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = fin; at != -1; at = prev[at]) {
            path.addFirst(at);
            if (at == inicio) break;
        }

        // si el primer elemento no es inicio => no hay camino
        if (path.isEmpty() || path.getFirst() != inicio) return Collections.emptyList();
        return path;
    }

    public String prim() {
        return "Resultado del algoritmo de Prim";
    }

    public String kruskal() {
        return "Resultado del algoritmo de Kruskal";
    }
}
