package com.nessaj.ersim.service;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RuntimeStateService {

    private final ConfigurationLoader configLoader;
    private final Map<String, DeviceRuntimeData> runtimeDataMap;
    private final Map<String, String> localNumToDeviceIdMap;
    private volatile boolean simulationRunning = false;

    public RuntimeStateService(ConfigurationLoader configLoader) {
        this.configLoader = configLoader;
        this.runtimeDataMap = new ConcurrentHashMap<>();
        this.localNumToDeviceIdMap = new ConcurrentHashMap<>();
    }

    public void initializeRuntimeData() {
        runtimeDataMap.clear();
        localNumToDeviceIdMap.clear();
        List<Device> devices = configLoader.getAllDevices();
        log.info("Initializing runtime data for {} devices", devices.size());
        for (Device device : devices) {
            log.debug("Creating runtime data for device: {} (type: {})", device.getId(), device.getType());
            DeviceRuntimeData runtimeData = createRuntimeData(device);
            runtimeDataMap.put(device.getId(), runtimeData);
            if (device.getDeviceLocalNum() != null && !device.getDeviceLocalNum().isEmpty()) {
                localNumToDeviceIdMap.put(device.getDeviceLocalNum(), device.getId());
            }
        }
        log.info("Runtime data initialized with {} devices", runtimeDataMap.size());
    }

    public String getDeviceIdByLocalNum(String localNum) {
        return localNumToDeviceIdMap.get(localNum);
    }

    private DeviceRuntimeData createRuntimeData(Device device) {
        DeviceRuntimeData runtimeData = new DeviceRuntimeData(device.getId());
        DeviceTypeDefinition typeDef = configLoader.getDeviceTypeDefinition(device.getType());

        if (typeDef != null && typeDef.getProperties() != null) {
            DeviceTypeProperties props = typeDef.getProperties();

            if (props.getTelemetry() != null) {
                for (PropertyDefinition prop : props.getTelemetry()) {
                    runtimeData.setTelemetryValue(prop.getKey(), prop.getDefaultValue());
                }
            }

            if (props.getTelesignal() != null) {
                for (PropertyDefinition prop : props.getTelesignal()) {
                    Object defaultVal = prop.getDefaultValue();
                    if (defaultVal instanceof Boolean) {
                        runtimeData.setTelesignalValue(prop.getKey(), ((Boolean) defaultVal) ? 1 : 0);
                    } else if (defaultVal instanceof Integer) {
                        runtimeData.setTelesignalValue(prop.getKey(), (Integer) defaultVal);
                    } else {
                        runtimeData.setTelesignalValue(prop.getKey(), 0);
                    }
                }
            }

            if (props.getTelecontrol() != null) {
                for (PropertyDefinition prop : props.getTelecontrol()) {
                    Object defaultVal = prop.getDefaultValue();
                    if (defaultVal instanceof Boolean) {
                        runtimeData.setTelecontrolValue(prop.getKey(), (Boolean) defaultVal);
                    } else {
                        runtimeData.setTelecontrolValue(prop.getKey(), false);
                    }
                }
            }

            if (props.getTeleadjust() != null) {
                for (PropertyDefinition prop : props.getTeleadjust()) {
                    runtimeData.setTeleadjustValue(prop.getKey(), prop.getDefaultValue());
                }
            }
        }

        return runtimeData;
    }

    public DeviceRuntimeData getRuntimeData(String deviceId) {
        return runtimeDataMap.get(deviceId);
    }

    public Map<String, DeviceRuntimeData> getAllRuntimeData() {
        return new HashMap<>(runtimeDataMap);
    }

    public void updateTelemetry(String deviceId, String key, Object value) {
        DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
        if (runtimeData != null) {
            runtimeData.setTelemetryValue(key, value);
        }
    }

    public void updateTelesignal(String deviceId, String key, Integer value) {
        DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
        if (runtimeData != null) {
            runtimeData.setTelesignalValue(key, value);
        }
    }

    public void updateTelecontrol(String deviceId, String key, Boolean value) {
        DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
        if (runtimeData != null) {
            runtimeData.setTelecontrolValue(key, value);
        }
    }

    public void updateTeleadjust(String deviceId, String key, Object value) {
        DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
        if (runtimeData != null) {
            runtimeData.setTeleadjustValue(key, value);
        }
    }

    public void addDeviceRuntimeData(Device device) {
        DeviceRuntimeData runtimeData = createRuntimeData(device);
        runtimeDataMap.put(device.getId(), runtimeData);
    }

    public void addRuntimeData(String deviceId, DeviceRuntimeData runtimeData) {
        runtimeDataMap.put(deviceId, runtimeData);
    }

    public void removeDeviceRuntimeData(String deviceId) {
        runtimeDataMap.remove(deviceId);
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public void setSimulationRunning(boolean running) {
        this.simulationRunning = running;
    }

    public void stopSimulation() {
        this.simulationRunning = false;
    }

    public void clearAllRuntimeData() {
        runtimeDataMap.clear();
    }
}