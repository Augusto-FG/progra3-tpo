package com.progra3_tpo.service.locationService;

import java.util.List;

public class CreateLocationRequest {

    private String nombre;
    private String tipo;
    private String direccion;
    private List<IncomingRouteRequest> incomingRoutes;

    public static class IncomingRouteRequest {
        private Long sourceId;
        private String nombreRuta;
        private double distancia;
        private double costo;
        private String tipoCamino;

        public IncomingRouteRequest() {}

        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

        public String getNombreRuta() { return nombreRuta; }
        public void setNombreRuta(String nombreRuta) { this.nombreRuta = nombreRuta; }

        public double getDistancia() { return distancia; }
        public void setDistancia(double distancia) { this.distancia = distancia; }

        public double getCosto() { return costo; }
        public void setCosto(double costo) { this.costo = costo; }

        public String getTipoCamino() { return tipoCamino; }
        public void setTipoCamino(String tipoCamino) { this.tipoCamino = tipoCamino; }
    }

    public CreateLocationRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public List<IncomingRouteRequest> getIncomingRoutes() { return incomingRoutes; }
    public void setIncomingRoutes(List<IncomingRouteRequest> incomingRoutes) { this.incomingRoutes = incomingRoutes; }
}