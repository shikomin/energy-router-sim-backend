package com.nessaj.ersim.model;

import java.util.ArrayList;
import java.util.List;

public class Bus {
    private String id;
    private String name;
    private String type;
    private int voltageLevel;          // 电压等级 (kV)，如 220, 110, 35, 10, 0.4
    private double ratedVoltage;       // 额定电压 (kV)
    private double ratedCurrent;       // 额定电流 (A)
    private double activePower;        // 有功功率 (MW)
    private double reactivePower;      // 无功功率 (MVar)
    private double voltage;            // 实际电压 (kV)
    private double current;            // 实际电流 (A)
    private List<String> connectedDevices;
    private String parentBusId;

    public Bus() {
        this.connectedDevices = new ArrayList<>();
        this.ratedVoltage = 0.4;
        this.ratedCurrent = 1000;
        this.voltageLevel = 0;
    }

    public Bus(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.connectedDevices = new ArrayList<>();
        this.ratedVoltage = 0.4;
        this.ratedCurrent = 1000;
        this.voltageLevel = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getVoltageLevel() { return voltageLevel; }
    public void setVoltageLevel(int voltageLevel) { this.voltageLevel = voltageLevel; }
    public double getRatedVoltage() { return ratedVoltage; }
    public void setRatedVoltage(double ratedVoltage) { this.ratedVoltage = ratedVoltage; }
    public double getRatedCurrent() { return ratedCurrent; }
    public void setRatedCurrent(double ratedCurrent) { this.ratedCurrent = ratedCurrent; }
    public double getActivePower() { return activePower; }
    public void setActivePower(double activePower) { this.activePower = activePower; }
    public double getReactivePower() { return reactivePower; }
    public void setReactivePower(double reactivePower) { this.reactivePower = reactivePower; }
    public double getVoltage() { return voltage; }
    public void setVoltage(double voltage) { this.voltage = voltage; }
    public double getCurrent() { return current; }
    public void setCurrent(double current) { this.current = current; }
    public List<String> getConnectedDevices() { return connectedDevices; }
    public void setConnectedDevices(List<String> connectedDevices) { this.connectedDevices = connectedDevices; }
    public String getParentBusId() { return parentBusId; }
    public void setParentBusId(String parentBusId) { this.parentBusId = parentBusId; }

    public void addConnectedDevice(String deviceId) {
        if (!this.connectedDevices.contains(deviceId)) {
            this.connectedDevices.add(deviceId);
        }
    }

    public void removeConnectedDevice(String deviceId) {
        this.connectedDevices.remove(deviceId);
    }
}