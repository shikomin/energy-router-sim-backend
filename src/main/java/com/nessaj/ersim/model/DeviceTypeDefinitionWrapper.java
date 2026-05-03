package com.nessaj.ersim.model;

import java.util.List;

public class DeviceTypeDefinitionWrapper {
    private List<DeviceTypeDefinition> deviceTypes;

    public DeviceTypeDefinitionWrapper() {}

    public List<DeviceTypeDefinition> getDeviceTypes() { return deviceTypes; }
    public void setDeviceTypes(List<DeviceTypeDefinition> deviceTypes) { this.deviceTypes = deviceTypes; }
}