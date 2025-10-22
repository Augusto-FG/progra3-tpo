package com.progra3_tpo.controller;

import com.progra3_tpo.service.GrafoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService grafoService;

    public GrafoController(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    @GetMapping("/bfs/{inicio}")
    public List<Integer> bfs(@PathVariable int inicio) {
        return grafoService.bfs(inicio);
    }

    @GetMapping("/dfs/{inicio}")
    public List<Integer> dfs(@PathVariable int inicio) {
        return grafoService.dfs(inicio);
    }

    @GetMapping("/dijkstra/{inicio}/{fin}")
    public List<Integer> dijkstra(@PathVariable int inicio, @PathVariable int fin) {
        return grafoService.dijkstra(inicio, fin);
    }

    @GetMapping("/prim")
    public String prim() {
        return grafoService.prim();
    }

    @GetMapping("/kruskal")
    public String kruskal() {
        return grafoService.kruskal();
    }
}
