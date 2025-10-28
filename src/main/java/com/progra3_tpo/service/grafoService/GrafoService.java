package com.progra3_tpo.service.grafoService;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GrafoService {

    private final int V = 6; // cantidad de nodos
    private final LinkedList<Integer>[] adj; // adyacencias

    public GrafoService() {
        adj = new LinkedList[V];
        for (int i = 0; i < V; i++) adj[i] = new LinkedList<>();

        // Ejemplo simple de red de rutas
        agregarArista(0, 1); // depósito → cliente A
        agregarArista(0, 2); // depósito → cliente B
        agregarArista(1, 3);
        agregarArista(2, 4);
        agregarArista(3, 5);
    }

    public void agregarArista(int v, int w) {
        adj[v].add(w);
        adj[w].add(v);
    }

    public List<Integer> bfs(int inicio) {
        List<Integer> recorrido = new ArrayList<>();
        boolean[] visitado = new boolean[V];
        Queue<Integer> cola = new LinkedList<>();
        visitado[inicio] = true;
        cola.add(inicio);

        while (!cola.isEmpty()) {
            int nodo = cola.poll();
            recorrido.add(nodo);
            for (int n : adj[nodo]) {
                if (!visitado[n]) {
                    visitado[n] = true;
                    cola.add(n);
                }
            }
        }
        return recorrido;
    }

    public List<Integer> dfs(int inicio) {
        List<Integer> recorrido = new ArrayList<>();
        boolean[] visitado = new boolean[V];
        dfsRec(inicio, visitado, recorrido);
        return recorrido;
    }

    private void dfsRec(int v, boolean[] visitado, List<Integer> recorrido) {
        visitado[v] = true;
        recorrido.add(v);
        for (int n : adj[v]) {
            if (!visitado[n]) dfsRec(n, visitado, recorrido);
        }
    }

    // placeholders que podés ir completando luego
    public List<Integer> dijkstra(int inicio, int fin) {
        return Arrays.asList(inicio, fin);
    }

    public String prim() {
        return "Resultado del algoritmo de Prim";
    }

    public String kruskal() {
        return "Resultado del algoritmo de Kruskal";
    }
}
