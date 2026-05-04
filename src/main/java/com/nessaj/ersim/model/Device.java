package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;

@Entity
@Table(name = "devices")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
    @Id
    private String id;
    private String name;
    private String type;
    private String ip;
    private Integer port;
    private String communicationType;
    private Integer slaveId;
    private String deviceLocalNum;
    private Boolean enabled;
    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

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
    public String getDeviceLocalNum() { return deviceLocalNum; }
    public void setDeviceLocalNum(String deviceLocalNum) { this.deviceLocalNum = deviceLocalNum; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
}