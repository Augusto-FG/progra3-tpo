// File: src/main/java/com/progra3_tpo/service/grafoService/DijkstraResult.java
package com.progra3_tpo.service.grafoService;

import java.util.List;

public class DijkstraResult {
    private List<Integer> path;
    private double cost;
    private String message;

    public DijkstraResult() {}

    public DijkstraResult(List<Integer> path, double cost, String message) {
        this.path = path;
        this.cost = cost;
        this.message = message;
    }

    public List<Integer> getPath() {
        return path;
    }

    public void setPath(List<Integer> path) {
        this.path = path;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
