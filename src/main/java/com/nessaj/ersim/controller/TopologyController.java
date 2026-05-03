package com.nessaj.ersim.controller;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.RuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topologies")
@CrossOrigin(origins = "*")
public class TopologyController {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;

    public TopologyController(ConfigurationLoader configLoader, RuntimeStateService runtimeStateService) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Topology>>> getAllTopologies() {
        List<Topology> topologies = configLoader.getAllTopologies();
        return ResponseEntity.ok(ApiResponse.success(topologies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Topology>> getTopology(@PathVariable String id) {
        Topology topology = configLoader.getTopology(id);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(topology));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Topology>> createTopology(@RequestBody Topology topology) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot create topology while simulation is running"));
        }

        if (configLoader.getTopology(topology.getId()) != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology already exists: " + topology.getId()));
        }

        configLoader.addTopology(topology);
        return ResponseEntity.ok(ApiResponse.success("Topology created successfully", topology));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Topology>> updateTopology(@PathVariable String id, @RequestBody Topology topology) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot update topology while simulation is running"));
        }

        if (!id.equals(topology.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology ID mismatch"));
        }

        Topology existingTopology = configLoader.getTopology(id);
        if (existingTopology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id));
        }

        configLoader.updateTopology(topology);
        return ResponseEntity.ok(ApiResponse.success("Topology updated successfully", topology));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTopology(@PathVariable String id) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete topology while simulation is running"));
        }

        Topology topology = configLoader.getTopology(id);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + id));
        }

        configLoader.deleteTopology(id);
        return ResponseEntity.ok(ApiResponse.success("Topology deleted successfully", null));
    }

    @PostMapping("/{topologyId}/buses")
    public ResponseEntity<ApiResponse<Bus>> addBus(@PathVariable String topologyId, @RequestBody Bus bus) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add bus while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        if (topology.getBus(bus.getId()) != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Bus already exists in topology: " + bus.getId()));
        }

        configLoader.addBusToTopology(topologyId, bus);
        return ResponseEntity.ok(ApiResponse.success("Bus added successfully", bus));
    }

    @DeleteMapping("/{topologyId}/buses/{busId}")
    public ResponseEntity<ApiResponse<Void>> removeBus(@PathVariable String topologyId, @PathVariable String busId) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot remove bus while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        if (topology.getBus(busId) == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Bus not found in topology: " + busId));
        }

        configLoader.removeBusFromTopology(topologyId, busId);
        return ResponseEntity.ok(ApiResponse.success("Bus removed successfully", null));
    }

    @PostMapping("/{topologyId}/connect")
    public ResponseEntity<ApiResponse<Void>> connectDeviceToBus(
            @PathVariable String topologyId,
            @RequestParam String deviceId,
            @RequestParam String busId) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot connect device while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        if (topology.getDevice(deviceId) == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found in topology: " + deviceId));
        }

        if (topology.getBus(busId) == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Bus not found in topology: " + busId));
        }

        configLoader.connectDeviceToBus(topologyId, deviceId, busId);
        return ResponseEntity.ok(ApiResponse.success("Device connected to bus successfully", null));
    }

    @PostMapping("/{topologyId}/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnectDeviceFromBus(
            @PathVariable String topologyId,
            @RequestParam String deviceId,
            @RequestParam String busId) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot disconnect device while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        configLoader.disconnectDeviceFromBus(topologyId, deviceId, busId);
        return ResponseEntity.ok(ApiResponse.success("Device disconnected from bus successfully", null));
    }

    @PostMapping("/{topologyId}/devices")
    public ResponseEntity<ApiResponse<Device>> addDeviceToTopology(
            @PathVariable String topologyId,
            @RequestBody Device device) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add device to topology while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        Device existingDevice = configLoader.getDevice(device.getId());
        if (existingDevice == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + device.getId()));
        }

        if (topology.getDevice(device.getId()) != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device already exists in topology: " + device.getId()));
        }

        topology.addDevice(existingDevice);
        configLoader.updateTopology(topology);
        return ResponseEntity.ok(ApiResponse.success("Device added to topology successfully", existingDevice));
    }

    @DeleteMapping("/{topologyId}/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> removeDeviceFromTopology(
            @PathVariable String topologyId,
            @PathVariable String deviceId) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot remove device from topology while simulation is running"));
        }

        Topology topology = configLoader.getTopology(topologyId);
        if (topology == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topology not found: " + topologyId));
        }

        topology.removeDevice(deviceId);
        configLoader.updateTopology(topology);
        return ResponseEntity.ok(ApiResponse.success("Device removed from topology successfully", null));
    }
}