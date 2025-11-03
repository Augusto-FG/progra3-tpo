package com.progra3_tpo.controller;

import com.progra3_tpo.service.PathRequest;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.backtrackingService.BacktrackingService;
import com.progra3_tpo.service.bfsService.BfsService;
import com.progra3_tpo.service.dfsService.DfsService;
import com.progra3_tpo.service.grafoService.GrafoService;
import com.progra3_tpo.service.primService.PrimService;
import com.progra3_tpo.service.kruscalService.KruscalService;
import com.progra3_tpo.service.ramificacion_podaService.Ramificacion_podaService;
import com.progra3_tpo.service.greedy.GreedyService;
import com.progra3_tpo.service.progradinamica.PrograDinamicaService;
import com.progra3_tpo.service.divideyconquista.DivideyConquistaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GrafoController {

    private final GrafoService grafoService;
    private final BacktrackingService backtrackingService;
    private final PrimService primService;
    private final KruscalService kruscalService;
    private final Ramificacion_podaService ramificacionPodaService;
    private final BfsService bfsService;
    private final DfsService dfsService;
    private final GreedyService greedyService;
    private final PrograDinamicaService prograDinamicaService;
    private final DivideyConquistaService divideyConquistaService;

    public GrafoController(GrafoService grafoService, BacktrackingService backtrackingService,
                           PrimService primService, KruscalService kruscalService,
                           Ramificacion_podaService ramificacionPodaService, BfsService bfsService, DfsService dfsService,
                           GreedyService greedyService, PrograDinamicaService prograDinamicaService,
                           DivideyConquistaService divideyConquistaService) {
        this.grafoService = grafoService;
        this.backtrackingService = backtrackingService;
        this.primService = primService;
        this.kruscalService = kruscalService;
        this.ramificacionPodaService = ramificacionPodaService;
        this.bfsService = bfsService;
        this.dfsService = dfsService;
        this.greedyService = greedyService;
        this.prograDinamicaService = prograDinamicaService;
        this.divideyConquistaService = divideyConquistaService;
    }

    @PostMapping("/dijkstra")
    public PathResponse computePath(
            @RequestBody PathRequest req,
            @RequestParam(required = false, defaultValue = "distance") String metric,
            @RequestParam(required = false) Double alpha
    ) {
        double alphaVal = (alpha == null) ? 0.5 : alpha;
        String metricVal = (metric == null || metric.isBlank()) ? "distance" : metric;
        return grafoService.computeWithDijkstra(req.getFrom(), req.getTo(), metricVal, alphaVal);
    }

    @PostMapping("/backtracking")
    public PathResponse computePathBacktracking(
            @RequestBody PathRequest req,
            @RequestParam(required = false, defaultValue = "distance") String metric,
            @RequestParam(required = false) Double alpha
    ) {
        double alphaVal = (alpha == null) ? 0.5 : alpha;
        String metricVal = (metric == null || metric.isBlank()) ? "distance" : metric;
        return backtrackingService.computeOptimalPath(req.getFrom(), req.getTo(), metricVal, alphaVal);
    }

    @PostMapping("/prim")
    public PathResponse computePathPrim(
            @RequestBody PathRequest req,
            @RequestParam(required = false, defaultValue = "distance") String metric,
            @RequestParam(required = false) Double alpha
    ) {
        double alphaVal = (alpha == null) ? 0.5 : alpha;
        String metricVal = (metric == null || metric.isBlank()) ? "distance" : metric;
        return primService.computeOptimalPath(req.getFrom(), req.getTo(), metricVal, alphaVal);
    }

    @PostMapping("/kruscal")
    public PathResponse computePathKruscal(
            @RequestBody PathRequest req,
            @RequestParam(required = false, defaultValue = "distance") String metric,
            @RequestParam(required = false) Double alpha
    ) {
        double alphaVal = (alpha == null) ? 0.5 : alpha;
        String metricVal = (metric == null || metric.isBlank()) ? "distance" : metric;
        return kruscalService.computeOptimalPath(req.getFrom(), req.getTo(), metricVal, alphaVal);
    }

    @PostMapping("/ramificacion_poda")
    public PathResponse computePathRamificacionPoda(
            @RequestBody PathRequest req,
            @RequestParam(required = false, defaultValue = "distance") String metric,
            @RequestParam(required = false) Double alpha
    ) {
        double alphaVal = (alpha == null) ? 0.5 : alpha;
        String metricVal = (metric == null || metric.isBlank()) ? "distance" : metric;
        return ramificacionPodaService.computeOptimalPath(req.getFrom(), req.getTo(), metricVal, alphaVal);
    }

    @PostMapping("/bfs")
    public PathResponse computePathBfs(@RequestBody PathRequest req) {
        return bfsService.computeBfsShortestHops(req.getFrom(), req.getTo());
    }

    @PostMapping("/dfs")
    public PathResponse computePathDfs(@RequestBody PathRequest req) {
        return dfsService.computeDfsPure(req.getFrom(), req.getTo());
    }



    @PostMapping("/greedy")
    public PathResponse computeGreedy(@RequestBody PathRequest request) {
        return greedyService.compute(request.getFrom(), request.getTo());
    }

    @PostMapping("/divideyconquista")
    public PathResponse computeDivideYConquista(@RequestBody PathRequest req) {
        return divideyConquistaService.compute(req.getFrom(), req.getTo());
    }

    @PostMapping("/programaciondinamica")
    public PathResponse computePathProgramacionDinamica(@RequestBody PathRequest req) {
        return prograDinamicaService.compute(req.getFrom(), req.getTo());
    }
}
