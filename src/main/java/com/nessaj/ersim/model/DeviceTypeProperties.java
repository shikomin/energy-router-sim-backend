package com.nessaj.ersim.model;

import java.util.List;

public class DeviceTypeProperties {
    private List<PropertyDefinition> telemetry;
    private List<PropertyDefinition> telesignal;
    private List<PropertyDefinition> telecontrol;
    private List<PropertyDefinition> teleadjust;

    public DeviceTypeProperties() {}

    public List<PropertyDefinition> getTelemetry() { return telemetry; }
    public void setTelemetry(List<PropertyDefinition> telemetry) { this.telemetry = telemetry; }
    public List<PropertyDefinition> getTelesignal() { return telesignal; }
    public void setTelesignal(List<PropertyDefinition> telesignal) { this.telesignal = telesignal; }
    public List<PropertyDefinition> getTelecontrol() { return telecontrol; }
    public void setTelecontrol(List<PropertyDefinition> telecontrol) { this.telecontrol = telecontrol; }
    public List<PropertyDefinition> getTeleadjust() { return teleadjust; }
    public void setTeleadjust(List<PropertyDefinition> teleadjust) { this.teleadjust = teleadjust; }
}