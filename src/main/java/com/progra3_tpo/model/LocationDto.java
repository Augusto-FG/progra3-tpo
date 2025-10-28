package com.progra3_tpo.model;

import org.springframework.data.neo4j.core.schema.*;
import java.util.List;

@Node("Location")
public class LocationDto {

    @Id
    @GeneratedValue
    private Long id;

    private String nombre;
    private String tipo;       // "DEPOSITO", "CLIENTE", "DISTRIBUIDOR"
    private String direccion;

    @Relationship(type = "RUTA_HACIA", direction = Relationship.Direction.OUTGOING)
    private List<RouteDto> rutas;

    @Relationship(type = "RUTA_HACIA", direction = Relationship.Direction.INCOMING)
    private List<RouteDto> rutasEntrantes;

    public LocationDto() {}

    public LocationDto(String nombre, String tipo, String direccion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.direccion = direccion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public List<RouteDto> getRutas() { return rutas; }
    public void setRutas(List<RouteDto> rutas) { this.rutas = rutas; }

    public List<RouteDto> getRutasEntrantes() { return rutasEntrantes; }
    public void setRutasEntrantes(List<RouteDto> rutasEntrantes) { this.rutasEntrantes = rutasEntrantes; }
}