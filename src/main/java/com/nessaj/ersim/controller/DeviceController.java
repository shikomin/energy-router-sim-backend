package com.nessaj.ersim.controller;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.repository.DeviceRepository;
import com.nessaj.ersim.service.RuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;

    public DeviceController(DeviceRepository deviceRepository, ConfigurationLoader configLoader, RuntimeStateService runtimeStateService) {
        this.deviceRepository = deviceRepository;
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Device>>> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Device>> getDevice(@PathVariable String id) {
        return deviceRepository.findById(id)
                .map(device -> ResponseEntity.ok(ApiResponse.success(device)))
                .orElse(ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Device>> createDevice(@RequestBody Device device) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add device while simulation is running"));
        }

        if (device.getId() == null || device.getId().trim().isEmpty()) {
            device.setId(UUID.randomUUID().toString());
        }

        if (deviceRepository.existsById(device.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device ID already exists: " + device.getId()));
        }

        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(device.getType());
        if (typeDef == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid device type: " + device.getType()));
        }

        Device savedDevice = deviceRepository.save(device);
        runtimeStateService.addDeviceRuntimeData(savedDevice);
        return ResponseEntity.ok(ApiResponse.success("Device created successfully", savedDevice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Device>> updateDevice(@PathVariable String id, @RequestBody Device device) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot update device while simulation is running"));
        }

        if (!id.equals(device.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device ID mismatch"));
        }

        if (!deviceRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id));
        }

        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(device.getType());
        if (typeDef == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid device type: " + device.getType()));
        }

        Device savedDevice = deviceRepository.save(device);
        runtimeStateService.removeDeviceRuntimeData(id);
        runtimeStateService.addDeviceRuntimeData(savedDevice);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", savedDevice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable String id) {
        if (runtimeStateService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete device while simulation is running"));
        }

        if (!deviceRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device not found: " + id));
        }

        deviceRepository.deleteById(id);
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