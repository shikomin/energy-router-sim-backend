package com.nessaj.ersim.model;

import java.util.List;

public class DeviceTypeDefinition {
    private String type;
    private String name;
    private String description;
    private String category;
    private List<CommunicationType> communicationTypes;
    private DeviceTypeProperties properties;

    public DeviceTypeDefinition() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<CommunicationType> getCommunicationTypes() { return communicationTypes; }
    public void setCommunicationTypes(List<CommunicationType> communicationTypes) { this.communicationTypes = communicationTypes; }
    public DeviceTypeProperties getProperties() { return properties; }
    public void setProperties(DeviceTypeProperties properties) { this.properties = properties; }
}