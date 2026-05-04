package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulatorConfig {
    private Integer stepIntervalMs;
    private Double timeScale;
    private Boolean enabled;

    public SimulatorConfig() {}

    public Integer getStepIntervalMs() { return stepIntervalMs; }
    public void setStepIntervalMs(Integer stepIntervalMs) { this.stepIntervalMs = stepIntervalMs; }
    public Double getTimeScale() { return timeScale; }
    public void setTimeScale(Double timeScale) { this.timeScale = timeScale; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}