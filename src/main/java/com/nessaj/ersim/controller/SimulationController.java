package com.nessaj.ersim.controller;

import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.SimulationControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationControlService simulationControlService;

    public SimulationController(SimulationControlService simulationControlService) {
        this.simulationControlService = simulationControlService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimulationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", simulationControlService.isRunning());
        status.put("simulationTime", simulationControlService.getSimulationTime());
        status.put("stepIntervalMs", simulationControlService.getStepIntervalMs());
        status.put("simulatorConfig", simulationControlService.getSimulatorConfig());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Void>> startSimulation() {
        if (simulationControlService.isRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Simulation is already running"));
        }

        simulationControlService.startSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation started successfully", null));
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<Void>> stopSimulation() {
        if (!simulationControlService.isRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Simulation is not running"));
        }

        simulationControlService.stopSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation stopped successfully", null));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetSimulation() {
        simulationControlService.resetSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation reset successfully", null));
    }

    @PostMapping("/step")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stepSimulation() {
        simulationControlService.stepSimulation();

        Map<String, Object> result = new HashMap<>();
        result.put("simulationTime", simulationControlService.getSimulationTime());
        result.put("running", simulationControlService.isRunning());

        return ResponseEntity.ok(ApiResponse.success("Simulation stepped successfully", result));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SimulatorConfig>> getSimulatorConfig() {
        return ResponseEntity.ok(ApiResponse.success(simulationControlService.getSimulatorConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<SimulatorConfig>> updateSimulatorConfig(@RequestBody SimulatorConfig config) {
        simulationControlService.updateSimulatorConfig(config);
        return ResponseEntity.ok(ApiResponse.success("Configuration updated successfully", config));
    }

    @PostMapping("/pbat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setPbatControl(@RequestBody Map<String, Object> request) {
        boolean manual = Boolean.TRUE.equals(request.get("manual"));
        double setpoint = 0;
        if (request.get("setpoint") != null) {
            setpoint = ((Number) request.get("setpoint")).doubleValue();
        }
        simulationControlService.setPbatManualMode(manual, setpoint);

        Map<String, Object> result = new HashMap<>();
        result.put("pbatManualMode", simulationControlService.isPbatManualMode());
        result.put("pbatManualSetpoint", simulationControlService.getPbatManualSetpoint());
        return ResponseEntity.ok(ApiResponse.success("Pbat control updated", result));
    }

    @GetMapping("/pbat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPbatControl() {
        Map<String, Object> result = new HashMap<>();
        result.put("pbatManualMode", simulationControlService.isPbatManualMode());
        result.put("pbatManualSetpoint", simulationControlService.getPbatManualSetpoint());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/system-state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setSystemState(@RequestBody Map<String, String> request) {
        String state = request.get("state");
        simulationControlService.setSystemState(state);

        Map<String, Object> result = new HashMap<>();
        result.put("systemState", simulationControlService.getSystemState());
        result.put("subState", simulationControlService.getCurrentSubState());
        return ResponseEntity.ok(ApiResponse.success("System state changed to " + state, result));
    }

    @GetMapping("/system-state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemState() {
        Map<String, Object> result = new HashMap<>();
        result.put("systemState", simulationControlService.getSystemState());
        result.put("subState", simulationControlService.getCurrentSubState());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}