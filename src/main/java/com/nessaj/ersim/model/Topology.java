package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Topology {
    private String id;
    private String name;
    private String description;
    private Map<String, Bus> buses;
    private Map<String, Device> devices;
    private List<String> deviceList;

    public Topology() {
        this.buses = new HashMap<>();
        this.devices = new HashMap<>();
        this.deviceList = new ArrayList<>();
    }

    public Topology(String id, String name) {
        this.id = id;
        this.name = name;
        this.buses = new HashMap<>();
        this.devices = new HashMap<>();
        this.deviceList = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Bus> getBuses() { return buses; }
    public void setBuses(Map<String, Bus> buses) { this.buses = buses; }
    public Map<String, Device> getDevices() { return devices; }
    public void setDevices(Map<String, Device> devices) { this.devices = devices; }
    public List<String> getDeviceList() { return deviceList; }
    public void setDeviceList(List<String> deviceList) { this.deviceList = deviceList; }

    public void addBus(Bus bus) {
        this.buses.put(bus.getId(), bus);
    }

    public void removeBus(String busId) {
        this.buses.remove(busId);
    }

    public Bus getBus(String busId) {
        return this.buses.get(busId);
    }

    public void addDevice(Device device) {
        this.devices.put(device.getId(), device);
        if (!this.deviceList.contains(device.getId())) {
            this.deviceList.add(device.getId());
        }
    }

    public void removeDevice(String deviceId) {
        this.devices.remove(deviceId);
        this.deviceList.remove(deviceId);
        for (Bus bus : this.buses.values()) {
            bus.removeConnectedDevice(deviceId);
        }
    }

    public Device getDevice(String deviceId) {
        return this.devices.get(deviceId);
    }

    public void connectDeviceToBus(String deviceId, String busId) {
        Device device = this.devices.get(deviceId);
        Bus bus = this.buses.get(busId);
        if (device != null && bus != null) {
            bus.addConnectedDevice(deviceId);
        }
    }

    public void disconnectDeviceFromBus(String deviceId, String busId) {
        Bus bus = this.buses.get(busId);
        if (bus != null) {
            bus.removeConnectedDevice(deviceId);
        }
    }

    public List<String> getDevicesOnBus(String busId) {
        Bus bus = this.buses.get(busId);
        return bus != null ? bus.getConnectedDevices() : new ArrayList<>();
    }
}