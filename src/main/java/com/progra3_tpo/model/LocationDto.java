package com.progra3_tpo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Location")
public class LocationDto {
    @Id
    @GeneratedValue
    private Long id;

    private String nombre;
    private String tipo; // "DEPOSITO", "CLIENTE", "DISTRIBUIDOR"
    private String direccion;

    // 🔗 Relaciones salientes hacia otras ubicaciones
    @Relationship(type = "CONECTA_A", direction = Relationship.Direction.OUTGOING)
    private List<RouteDto> rutas;
}
