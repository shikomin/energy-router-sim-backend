package com.nessaj.ersim.model;

public class DefaultsConfig {
    private Double acVoltage;
    private Double acFrequency;
    private Double dcVoltage;

    public DefaultsConfig() {}

    public Double getAcVoltage() { return acVoltage; }
    public void setAcVoltage(Double acVoltage) { this.acVoltage = acVoltage; }
    public Double getAcFrequency() { return acFrequency; }
    public void setAcFrequency(Double acFrequency) { this.acFrequency = acFrequency; }
    public Double getDcVoltage() { return dcVoltage; }
    public void setDcVoltage(Double dcVoltage) { this.dcVoltage = dcVoltage; }
}