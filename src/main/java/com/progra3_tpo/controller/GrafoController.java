package com.progra3_tpo.controller;

import com.progra3_tpo.service.grafoService.DijkstraResult;
import com.progra3_tpo.service.grafoService.GrafoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService grafoService;

    public GrafoController(GrafoService grafoService) {
        this.grafoService = grafoService;
    }


    @GetMapping("/dijkstra/{nombreInicio}/{nombreFin}")
    public DijkstraResult dijkstra(@PathVariable String nombreInicio, @PathVariable String nombreFin) {
        return grafoService.dijkstra(nombreInicio, nombreFin);
    }


}
