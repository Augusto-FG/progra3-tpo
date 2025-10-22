package com.progra3_tpo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RelationshipProperties
public class RouteDto {
    @Id
    @GeneratedValue
    private Long id;

    private double distancia;
    private double costo;

    @TargetNode
    private NodoDto destino;
}
