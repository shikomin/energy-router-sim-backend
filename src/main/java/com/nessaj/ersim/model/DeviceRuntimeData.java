package com.nessaj.ersim.model;

import java.util.HashMap;
import java.util.Map;

public class DeviceRuntimeData {
    private String deviceId;
    private Map<String, Object> telemetry;
    private Map<String, Integer> telesignal;
    private Map<String, Boolean> telecontrol;
    private Map<String, Object> teleadjust;

    public DeviceRuntimeData() {
        this.telemetry = new HashMap<>();
        this.telesignal = new HashMap<>();
        this.telecontrol = new HashMap<>();
        this.teleadjust = new HashMap<>();
    }

    public DeviceRuntimeData(String deviceId) {
        this.deviceId = deviceId;
        this.telemetry = new HashMap<>();
        this.telesignal = new HashMap<>();
        this.telecontrol = new HashMap<>();
        this.teleadjust = new HashMap<>();
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Map<String, Object> getTelemetry() { return telemetry; }
    public void setTelemetry(Map<String, Object> telemetry) { this.telemetry = telemetry; }
    public Map<String, Integer> getTelesignal() { return telesignal; }
    public void setTelesignal(Map<String, Integer> telesignal) { this.telesignal = telesignal; }
    public Map<String, Boolean> getTelecontrol() { return telecontrol; }
    public void setTelecontrol(Map<String, Boolean> telecontrol) { this.telecontrol = telecontrol; }
    public Map<String, Object> getTeleadjust() { return teleadjust; }
    public void setTeleadjust(Map<String, Object> teleadjust) { this.teleadjust = teleadjust; }

    public void setTelemetryValue(String key, Object value) {
        this.telemetry.put(key, value);
    }

    public void setTelesignalValue(String key, Integer value) {
        this.telesignal.put(key, value);
    }

    public void setTelecontrolValue(String key, Boolean value) {
        this.telecontrol.put(key, value);
    }

    public void setTeleadjustValue(String key, Object value) {
        this.teleadjust.put(key, value);
    }

    public Object getTelemetryValue(String key) {
        return this.telemetry.get(key);
    }

    public Integer getTelesignalValue(String key) {
        return this.telesignal.get(key);
    }

    public Boolean getTelecontrolValue(String key) {
        return this.telecontrol.get(key);
    }

    public Object getTeleadjustValue(String key) {
        return this.teleadjust.get(key);
    }

    /**
     * 将对象转换为 Map，用于 WebSocket 发布
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceId", deviceId);
        map.put("telemetry", telemetry);
        map.put("telesignal", telesignal);
        map.put("telecontrol", telecontrol);
        map.put("teleadjust", teleadjust);
        return map;
    }
}