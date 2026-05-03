package com.nessaj.ersim.model;

public enum CommunicationType {
    MODBUS_TCP("Modbus-TCP"),
    MODBUS_RTU("Modbus-RTU");

    private final String description;

    CommunicationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}