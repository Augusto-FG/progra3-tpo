package com.progra3_tpo.repository;

import com.progra3_tpo.model.LocationDto;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LocationRepository extends Neo4jRepository<LocationDto, Long> {
}
