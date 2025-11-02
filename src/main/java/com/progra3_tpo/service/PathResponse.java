package com.progra3_tpo.service;

import java.util.List;

public class PathResponse {
    private String message;                       // mensaje con nombres de locations/routes y totales
    private List<String> nodosARecorrer;          // antes 'nodes'
    private List<String> aristasARecorrer;        // antes 'routes'
    private double totalDistance;
    private double totalCost;

    public PathResponse() {}

    public PathResponse(String message, List<String> nodosARecorrer, List<String> aristasARecorrer) {
        this.message = message;
        this.nodosARecorrer = nodosARecorrer;
        this.aristasARecorrer = aristasARecorrer;
    }

    public PathResponse(String message, List<String> nodosARecorrer, List<String> aristasARecorrer,
                        double totalDistance, double totalCost) {
        this.message = message;
        this.nodosARecorrer = nodosARecorrer;
        this.aristasARecorrer = aristasARecorrer;
        this.totalDistance = totalDistance;
        this.totalCost = totalCost;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getNodosARecorrer() { return nodosARecorrer; }
    public void setNodosARecorrer(List<String> nodosARecorrer) { this.nodosARecorrer = nodosARecorrer; }

    public List<String> getAristasARecorrer() { return aristasARecorrer; }
    public void setAristasARecorrer(List<String> aristasARecorrer) { this.aristasARecorrer = aristasARecorrer; }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
}
