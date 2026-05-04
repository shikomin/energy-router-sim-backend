package com.nessaj.ersim.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.repository.TopologyRepository;
import com.nessaj.ersim.service.RuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/topologies")
@CrossOrigin(origins = "*")
public class TopologyController {

    private final TopologyRepository topologyRepository;
    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TopologyController(TopologyRepository topologyRepository, ConfigurationLoader configLoader, RuntimeStateService runtimeStateService) {
        this.topologyRepository = topologyRepository;
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Topology>>> getAllTopologies() {
        List<Topology> topologies = topologyRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(topologies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Topology>> getTopology(@PathVariable String id) {
        return topologyRepository.findById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElse(ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Topology>> createTopology(@RequestBody Topology topology) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot create topology while simulation is running"));
        }

        if (topologyRepository.existsById(topology.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology already exists: " + topology.getId()));
        }

        serializeTopologyData(topology);
        return ResponseEntity.ok(ApiResponse.success("Topology created successfully", topologyRepository.save(topology)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Topology>> updateTopology(@PathVariable String id, @RequestBody Topology topology) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot update topology while simulation is running"));
        }

        if (!id.equals(topology.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology ID mismatch"));
        }

        if (!topologyRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id));
        }

        serializeTopologyData(topology);
        return ResponseEntity.ok(ApiResponse.success("Topology updated successfully", topologyRepository.save(topology)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTopology(@PathVariable String id) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete topology while simulation is running"));
        }

        if (!topologyRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id));
        }

        topologyRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Topology deleted successfully", null));
    }

    private void serializeTopologyData(Topology topology) {
        try {
            topology.setBusesJson(objectMapper.writeValueAsString(topology.getBuses()));
            topology.setDevicesJson(objectMapper.writeValueAsString(topology.getDevices()));
            topology.setDeviceListJson(objectMapper.writeValueAsString(topology.getDeviceList()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize topology data", e);
        }
    }
}