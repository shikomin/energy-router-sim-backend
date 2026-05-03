package com.nessaj.ersim.model;

import java.util.List;

public class PropertyDefinition {
    private String key;
    private String name;
    private String unit;
    private Object defaultValue;
    private Double min;
    private Double max;

    public PropertyDefinition() {}

    public PropertyDefinition(String key, String name, String unit, Object defaultValue) {
        this.key = key;
        this.name = name;
        this.unit = unit;
        this.defaultValue = defaultValue;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    public Double getMin() { return min; }
    public void setMin(Double min) { this.min = min; }
    public Double getMax() { return max; }
    public void setMax(Double max) { this.max = max; }
}