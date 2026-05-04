package com.nessaj.ersim.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.repository.DeviceRepository;
import com.nessaj.ersim.repository.PointRepository;
import com.nessaj.ersim.repository.TopologyRepository;
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

    private final DeviceRepository deviceRepository;
    private final PointRepository pointRepository;
    private final TopologyRepository topologyRepository;

    private final Map<String, Device> deviceMap;
    private final Map<String, DeviceTypeDefinition> deviceTypeMap;
    private final Map<String, Topology> topologyMap;
    private final Map<String, Point> pointMap;
    private final ObjectMapper objectMapper;
    private final String configPath;

    public ConfigurationLoader(DeviceRepository deviceRepository,
                               PointRepository pointRepository,
                               TopologyRepository topologyRepository) {
        this.deviceRepository = deviceRepository;
        this.pointRepository = pointRepository;
        this.topologyRepository = topologyRepository;
        this.deviceMap = new HashMap<>();
        this.deviceTypeMap = new HashMap<>();
        this.topologyMap = new HashMap<>();
        this.pointMap = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.configPath = System.getProperty("config.path", "src/main/resources/config");
    }

    @PostConstruct
    public void loadAllConfigurations() {
        loadDeviceTypes();
        importDevicesFromJson();
        importTopologiesFromJson();
        importPointsFromJson();
        initializeTopologyDevices();
    }

    private void importDevicesFromJson() {
        if (deviceRepository.count() == 0) {
            log.info("Device database is empty, importing from JSON...");
            List<Device> devices = loadDevicesFromJson();
            if (!devices.isEmpty()) {
                deviceRepository.saveAll(devices);
                log.info("Imported {} devices from JSON to database", devices.size());
            }
        } else {
            log.info("Device database already has data, skipping JSON import");
        }
    }

    private List<Device> loadDevicesFromJson() {
        try {
            File file = new File(configPath, "device.json");
            if (!file.exists()) {
                log.warn("Device configuration file not found, returning empty list");
                return List.of();
            }
            DeviceWrapper wrapper = objectMapper.readValue(file, DeviceWrapper.class);
            if (wrapper != null && wrapper.getDevices() != null) {
                deviceMap.clear();
                for (Device device : wrapper.getDevices()) {
                    deviceMap.put(device.getId(), device);
                }
                return wrapper.getDevices();
            }
        } catch (IOException e) {
            log.error("Failed to load devices configuration", e);
        }
        return List.of();
    }

    private void importTopologiesFromJson() {
        if (topologyRepository.count() == 0) {
            log.info("Topology database is empty, importing from JSON...");
            List<Topology> topologies = loadTopologiesFromJson();
            if (!topologies.isEmpty()) {
                for (Topology topology : topologies) {
                    serializeTopologyData(topology);
                }
                topologyRepository.saveAll(topologies);
                log.info("Imported {} topologies from JSON to database", topologies.size());
            }
        } else {
            log.info("Topology database already has data, skipping JSON import");
        }
    }

    private void serializeTopologyData(Topology topology) {
        try {
            topology.setBusesJson(objectMapper.writeValueAsString(topology.getBuses()));
            topology.setDevicesJson(objectMapper.writeValueAsString(topology.getDevices()));
            topology.setDeviceListJson(objectMapper.writeValueAsString(topology.getDeviceList()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize topology data for topology: {}", topology.getId(), e);
        }
    }

    private List<Topology> loadTopologiesFromJson() {
        try {
            File file = new File(configPath, "topology.json");
            if (!file.exists()) {
                log.warn("Topology configuration file not found, returning empty list");
                return List.of();
            }
            TopologyWrapper wrapper = objectMapper.readValue(file, TopologyWrapper.class);
            if (wrapper != null && wrapper.getTopologies() != null) {
                topologyMap.clear();
                for (Topology topology : wrapper.getTopologies()) {
                    topologyMap.put(topology.getId(), topology);
                }
                return wrapper.getTopologies();
            }
        } catch (IOException e) {
            log.error("Failed to load topologies configuration", e);
        }
        return List.of();
    }

    private void importPointsFromJson() {
        if (pointRepository.count() == 0) {
            log.info("Point database is empty, importing from JSON...");
            List<Point> points = loadPointsFromJson();
            pointRepository.saveAll(points);
            log.info("Imported {} points from JSON to database", points.size());
        } else {
            log.info("Point database already has data, skipping JSON import");
        }
    }

    private List<Point> loadPointsFromJson() {
        try {
            File file = new File(configPath, "point.json");
            if (!file.exists()) {
                log.warn("Point configuration file not found, returning empty list");
                return List.of();
            }
            PointWrapper wrapper = objectMapper.readValue(file, PointWrapper.class);
            if (wrapper != null && wrapper.getPoints() != null) {
                pointMap.clear();
                for (Point point : wrapper.getPoints()) {
                    String id = point.getPtId() + "_" + point.getSignalType() + "_" + point.getDeviceLocalNum();
                    point.setId(id);
                    pointMap.put(id, point);
                }
                return wrapper.getPoints();
            }
        } catch (IOException e) {
            log.error("Failed to load points configuration", e);
        }
        return List.of();
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

    public List<Point> loadPoints() {
        return pointRepository.findAll();
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