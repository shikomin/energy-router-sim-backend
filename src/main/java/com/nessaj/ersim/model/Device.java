package com.nessaj.ersim.model;

import java.util.Map;

public class Device {
    private String id;
    private String name;
    private String type;
    private String ip;
    private Integer port;
    private String communicationType;
    private Integer slaveId;
    private Map<String, DeviceConnection> connections;

    public Device() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getCommunicationType() { return communicationType; }
    public void setCommunicationType(String communicationType) { this.communicationType = communicationType; }
    public Integer getSlaveId() { return slaveId; }
    public void setSlaveId(Integer slaveId) { this.slaveId = slaveId; }
    public Map<String, DeviceConnection> getConnections() { return connections; }
    public void setConnections(Map<String, DeviceConnection> connections) { this.connections = connections; }
}