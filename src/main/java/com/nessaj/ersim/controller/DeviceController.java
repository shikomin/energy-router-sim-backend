package com.nessaj.ersim.controller;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.RuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;

    public DeviceController(ConfigurationLoader configLoader, RuntimeStateService runtimeStateService) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Device>>> getAllDevices() {
        List<Device> devices = configLoader.getAllDevices();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Device>> getDevice(@PathVariable String id) {
        Device device = configLoader.getDevice(id);
        if (device == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Device>> createDevice(@RequestBody Device device) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add device while simulation is running"));
        }

        if (device.getId() == null || device.getId().trim().isEmpty()) {
            device.setId(UUID.randomUUID().toString());
        }

        if (configLoader.getDevice(device.getId()) != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device ID already exists: " + device.getId()));
        }

        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(device.getType());
        if (typeDef == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid device type: " + device.getType()));
        }

        configLoader.addDevice(device);
        runtimeStateService.addDeviceRuntimeData(device);
        return ResponseEntity.ok(ApiResponse.success("Device created successfully", device));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Device>> updateDevice(@PathVariable String id, @RequestBody Device device) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot update device while simulation is running"));
        }

        if (!id.equals(device.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device ID mismatch"));
        }

        Device existingDevice = configLoader.getDevice(id);
        if (existingDevice == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id));
        }

        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(device.getType());
        if (typeDef == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid device type: " + device.getType()));
        }

        configLoader.updateDevice(device);
        runtimeStateService.removeDeviceRuntimeData(id);
        runtimeStateService.addDeviceRuntimeData(device);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", device));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable String id) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete device while simulation is running"));
        }

        Device device = configLoader.getDevice(id);
        if (device == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id));
        }

        configLoader.deleteDevice(id);
        runtimeStateService.removeDeviceRuntimeData(id);
        return ResponseEntity.ok(ApiResponse.success("Device deleted successfully", null));
    }

    @GetMapping("/{id}/runtime")
    public ResponseEntity<ApiResponse<DeviceRuntimeData>> getDeviceRuntimeData(@PathVariable String id) {
        DeviceRuntimeData runtimeData = runtimeStateService.getRuntimeData(id);
        if (runtimeData == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Runtime data not found for device: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(runtimeData));
    }

    @GetMapping("/runtime")
    public ResponseEntity<ApiResponse<List<DeviceRuntimeData>>> getAllDeviceRuntimeData() {
        List<DeviceRuntimeData> allRuntimeData = List.copyOf(runtimeStateService.getAllRuntimeData().values());
        return ResponseEntity.ok(ApiResponse.success(allRuntimeData));
    }
}