package com.progra3_tpo.controller;

import com.progra3_tpo.service.PathRequest;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.backtrackingService.BacktrackingService;
import com.progra3_tpo.service.grafoService.GrafoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GrafoController {

    private final GrafoService grafoService;
    private final BacktrackingService backtrackingService;

    public GrafoController(GrafoService grafoService, BacktrackingService backtrackingService) {
        this.grafoService = grafoService;
        this.backtrackingService = backtrackingService;
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
}
