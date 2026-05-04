package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceTypeDefinitionWrapper {
    private List<DeviceTypeDefinition> deviceTypes;

    public DeviceTypeDefinitionWrapper() {}

    public List<DeviceTypeDefinition> getDeviceTypes() { return deviceTypes; }
    public void setDeviceTypes(List<DeviceTypeDefinition> deviceTypes) { this.deviceTypes = deviceTypes; }
}