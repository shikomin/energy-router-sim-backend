package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceWrapper {
    private List<Device> devices;

    public DeviceWrapper() {}

    public List<Device> getDevices() { return devices; }
    public void setDevices(List<Device> devices) { this.devices = devices; }
}