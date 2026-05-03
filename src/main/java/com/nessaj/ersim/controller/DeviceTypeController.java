package com.nessaj.ersim.controller;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.RuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device-types")
@CrossOrigin(origins = "*")
public class DeviceTypeController {

    private final ConfigurationLoader configLoader;

    public DeviceTypeController(ConfigurationLoader configLoader) {
        this.configLoader = configLoader;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceTypeDefinition>>> getAllDeviceTypes() {
        List<DeviceTypeDefinition> deviceTypes = configLoader.getAllDeviceTypeDefinitions();
        return ResponseEntity.ok(ApiResponse.success(deviceTypes));
    }

    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<DeviceTypeDefinition>> getDeviceType(@PathVariable String type) {
        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(type);
        if (typeDef == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Device type not found: " + type));
        }
        return ResponseEntity.ok(ApiResponse.success(typeDef));
    }
}