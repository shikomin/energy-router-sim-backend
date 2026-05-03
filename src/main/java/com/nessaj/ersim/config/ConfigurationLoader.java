package com.nessaj.ersim.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.nessaj.ersim.model.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ConfigurationLoader {

    private final ObjectMapper objectMapper;
    private final Map<String, DeviceTypeDefinition> deviceTypeMap;
    private final Map<String, Device> deviceMap;
    private final Map<String, Topology> topologyMap;

    private String configPath;

    public ConfigurationLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.deviceTypeMap = new ConcurrentHashMap<>();
        this.deviceMap = new ConcurrentHashMap<>();
        this.topologyMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        this.configPath = System.getProperty("config.path", "src/main/resources/config");
        loadAllConfigurations();
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
        loadAllConfigurations();
    }

    private void loadAllConfigurations() {
        loadDeviceTypes();
        loadDevices();
        loadTopologies();
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
            if (!file.exists()) {
                return;
            }
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
            throw new RuntimeException("Failed to save devices configuration", e);
        }
    }

    public void saveTopologies() {
        try {
            File file = new File(configPath, "topology.json");
            TopologyWrapper wrapper = new TopologyWrapper();
            wrapper.setTopologies(topologyMap.values().stream().collect(Collectors.toList()));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, wrapper);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save topologies configuration", e);
        }
    }

    public DeviceTypeDefinition getDeviceTypeDefinition(String type) {
        return deviceTypeMap.get(type);
    }

    public List<DeviceTypeDefinition> getAllDeviceTypeDefinitions() {
        return deviceTypeMap.values().stream().collect(Collectors.toList());
    }

    public Device getDevice(String id) {
        return deviceMap.get(id);
    }

    public List<Device> getAllDevices() {
        return deviceMap.values().stream().collect(Collectors.toList());
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

    public Topology getTopology(String id) {
        return topologyMap.get(id);
    }

    public List<Topology> getAllTopologies() {
        return topologyMap.values().stream().collect(Collectors.toList());
    }

    public void addTopology(Topology topology) {
        topologyMap.put(topology.getId(), topology);
        saveTopologies();
    }

    public void updateTopology(Topology topology) {
        if (topologyMap.containsKey(topology.getId())) {
            topologyMap.put(topology.getId(), topology);
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

    public SimulatorConfigWrapper loadSimulatorConfig() {
        try {
            File file = new File(configPath, "simulator.json");
            return objectMapper.readValue(file, SimulatorConfigWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load simulator configuration", e);
        }
    }

    public void saveSimulatorConfig(SimulatorConfigWrapper config) {
        try {
            File file = new File(configPath, "simulator.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save simulator configuration", e);
        }
    }
}