package com.progra3_tpo.service.grafoService;

import com.progra3_tpo.model.LocationDto;
import com.progra3_tpo.model.RouteDto;
import com.progra3_tpo.repository.LocationRepository;
import com.progra3_tpo.service.PathResponse;
import com.progra3_tpo.service.dikstraService.DijkstraService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrafoService {

    private final LocationRepository locationRepository;
    private final DijkstraService dijkstraService;

    public GrafoService(LocationRepository locationRepository, DijkstraService dijkstraService) {
        this.locationRepository = locationRepository;
        this.dijkstraService = dijkstraService;
    }

    public PathResponse computeWithDijkstra(String from, String to, String metric, double alpha) {
        List<LocationDto> nodes = locationRepository.findAll();
        int n = nodes.size();
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            nameToIndex.put(nodes.get(i).getNombre(), i);
        }

        List<List<DijkstraService.EdgeDto>> adjList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjList.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            LocationDto src = nodes.get(i);
            List<RouteDto> rutas = src.getRutas();
            if (rutas == null) continue;
            for (RouteDto r : rutas) {
                if (r == null || r.getDestino() == null || r.getDestino().getNombre() == null) continue;
                Integer destIdx = nameToIndex.get(r.getDestino().getNombre());
                if (destIdx == null) continue;
                adjList.get(i).add(new DijkstraService.EdgeDto(destIdx, r.getDistancia(), r.getCosto(), r));
            }
        }

        Integer s = nameToIndex.get(from);
        Integer t = nameToIndex.get(to);
        if (s == null || t == null) {
            return new PathResponse("Inicio o destino no encontrado", new ArrayList<>(), new ArrayList<>(), 0.0, 0.0);
        }

        String m = (metric == null) ? "distance" : metric;
        if (Double.isNaN(alpha)) alpha = 0.5;
        return dijkstraService.compute(s, t, adjList, nodes, m, alpha);
    }
}
