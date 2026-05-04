package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceConnection {
    private String busId;
    private String port;

    public DeviceConnection() {}

    public DeviceConnection(String busId, String port) {
        this.busId = busId;
        this.port = port;
    }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
}