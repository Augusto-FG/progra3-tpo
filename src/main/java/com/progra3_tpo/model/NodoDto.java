package com.progra3_tpo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Nodo")
public class NodoDto {
    @Id
    @GeneratedValue
    private Long id;

    private String nombre;
    private String tipo; // "DEPOSITO", "CLIENTE", "DISTRIBUIDOR"
    private String direccion;
}
