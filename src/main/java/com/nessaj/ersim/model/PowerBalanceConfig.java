package com.nessaj.ersim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerBalanceConfig {
    private Double tolerance;
    private Integer maxIterations;

    public PowerBalanceConfig() {}

    public Double getTolerance() { return tolerance; }
    public void setTolerance(Double tolerance) { this.tolerance = tolerance; }
    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }
}