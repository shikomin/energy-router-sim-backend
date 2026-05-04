package com.nessaj.ersim.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nessaj.ersim.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConfigurationLoader {

    private final Map<String, Device> deviceMap;
    private final Map<String, DeviceTypeDefinition> deviceTypeMap;
    private final Map<String, Topology> topologyMap;
    private final ObjectMapper objectMapper;
    private final String configPath;

    public ConfigurationLoader() {
        this.deviceMap = new HashMap<>();
        this.deviceTypeMap = new HashMap<>();
        this.topologyMap = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.configPath = System.getProperty("config.path", "src/main/resources/config");
    }

    @PostConstruct
    public void loadAllConfigurations() {
        loadDeviceTypes();
        loadDevices();
        loadTopologies();
        initializeTopologyDevices();
    }

    private void initializeTopologyDevices() {
        for (Topology topology : topologyMap.values()) {
            if (topology.getDevices() == null || topology.getDevices().isEmpty()) {
                topology.setDevices(new HashMap<>());
            }
            for (String deviceId : topology.getDeviceList()) {
                Device device = deviceMap.get(deviceId);
                if (device != null) {
                    topology.getDevices().put(deviceId, device);
                }
            }
            log.info("Initialized topology {} with {} devices", topology.getId(), topology.getDevices().size());
        }
    }

    public void loadDeviceTypes() {
        try {
            File file = new File(configPath, "device_type.json");
            DeviceTypeDefinitionWrapper wrapper = objectMapper.readValue(file, DeviceTypeDefinitionWrapper.class);
            if (wrapper != null && wrapper.getDeviceTypes() != null) {
                deviceTypeMap.clear();
                for (DeviceTypeDefinition typeDef : wrapper.getDeviceTypes()) {
                    deviceTypeMap.put(typeDef.getType(), typeDef);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load device types configuration", e);
        }
    }

    public void loadDevices() {
        try {
            File file = new File(configPath, "device.json");
            DeviceWrapper wrapper = objectMapper.readValue(file, DeviceWrapper.class);
            if (wrapper != null && wrapper.getDevices() != null) {
                deviceMap.clear();
                for (Device device : wrapper.getDevices()) {
                    deviceMap.put(device.getId(), device);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load devices configuration", e);
        }
    }

    public void loadTopologies() {
        try {
            File file = new File(configPath, "topology.json");
            if (!file.exists()) {
                TopologyWrapper wrapper = new TopologyWrapper();
                objectMapper.writeValue(file, wrapper);
                return;
            }
            TopologyWrapper wrapper = objectMapper.readValue(file, TopologyWrapper.class);
            if (wrapper != null && wrapper.getTopologies() != null) {
                topologyMap.clear();
                for (Topology topology : wrapper.getTopologies()) {
                    topologyMap.put(topology.getId(), topology);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load topologies configuration", e);
        }
    }

    public void saveDevices() {
        try {
            File file = new File(configPath, "device.json");
            DeviceWrapper wrapper = new DeviceWrapper();
            wrapper.setDevices(deviceMap.values().stream().collect(Collectors.toList()));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, wrapper);
        } catch (IOException e) {
            log.error("Failed to save devices configuration", e);
        }
    }

    public void saveSimulatorConfig(SimulatorConfigWrapper config) {
        try {
            File file = new File(configPath, "simulator.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
        } catch (IOException e) {
            log.error("Failed to save simulator configuration", e);
        }
    }

    public SimulatorConfigWrapper loadSimulatorConfig() {
        try {
            File file = new File(configPath, "simulator.json");
            if (file.exists()) {
                return objectMapper.readValue(file, SimulatorConfigWrapper.class);
            }
        } catch (IOException e) {
            log.error("Failed to load simulator configuration", e);
        }
        return null;
    }

    public void saveTopologies() {
        try {
            File file = new File(configPath, "topology.json");
            TopologyWrapper wrapper = new TopologyWrapper();
            wrapper.setTopologies(topologyMap.values().stream().collect(Collectors.toList()));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, wrapper);
        } catch (IOException e) {
            log.error("Failed to save topologies configuration", e);
        }
    }

    public DeviceTypeDefinition getDeviceTypeDefinition(String type) {
        return deviceTypeMap.get(type);
    }

    public Device getDevice(String id) {
        return deviceMap.get(id);
    }

    public List<Device> getAllDevices() {
        return deviceMap.values().stream().collect(Collectors.toList());
    }

    public Map<String, Device> getDeviceMap() {
        return new HashMap<>(deviceMap);
    }

    public void addDevice(Device device) {
        deviceMap.put(device.getId(), device);
        saveDevices();
    }

    public void updateDevice(Device device) {
        if (deviceMap.containsKey(device.getId())) {
            deviceMap.put(device.getId(), device);
            saveDevices();
        }
    }

    public void deleteDevice(String id) {
        deviceMap.remove(id);
        saveDevices();
    }

    public List<DeviceTypeDefinition> getAllDeviceTypes() {
        return deviceTypeMap.values().stream().collect(Collectors.toList());
    }

    public Topology getTopology(String id) {
        return topologyMap.get(id);
    }

    public List<Topology> getAllTopologies() {
        return topologyMap.values().stream().collect(Collectors.toList());
    }

    public void addTopology(Topology topology) {
        topologyMap.put(topology.getId(), topology);
        initializeTopologyDevices();
        saveTopologies();
    }

    public void updateTopology(Topology topology) {
        if (topologyMap.containsKey(topology.getId())) {
            topologyMap.put(topology.getId(), topology);
            initializeTopologyDevices();
            saveTopologies();
        }
    }

    public void deleteTopology(String id) {
        topologyMap.remove(id);
        saveTopologies();
    }

    public void addBusToTopology(String topologyId, Bus bus) {
        Topology topology = topologyMap.get(topologyId);
        if (topology != null) {
            topology.addBus(bus);
            saveTopologies();
        }
    }

    public void removeBusFromTopology(String topologyId, String busId) {
        Topology topology = topologyMap.get(topologyId);
        if (topology != null) {
            topology.removeBus(busId);
            saveTopologies();
        }
    }

    public void connectDeviceToBus(String topologyId, String deviceId, String busId) {
        Topology topology = topologyMap.get(topologyId);
        if (topology != null) {
            topology.connectDeviceToBus(deviceId, busId);
            saveTopologies();
        }
    }

    public void disconnectDeviceFromBus(String topologyId, String deviceId, String busId) {
        Topology topology = topologyMap.get(topologyId);
        if (topology != null) {
            topology.disconnectDeviceFromBus(deviceId, busId);
            saveTopologies();
        }
    }
}