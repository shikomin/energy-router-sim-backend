package com.nessaj.ersim.repository;

import com.nessaj.ersim.model.Topology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopologyRepository extends JpaRepository<Topology, String> {
}