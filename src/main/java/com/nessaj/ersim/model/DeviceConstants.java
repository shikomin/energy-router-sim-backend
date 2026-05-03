package com.nessaj.ersim.model;

public class DeviceConstants {

    public static final String DEVICE_TYPE_PCS = "PCS";
    public static final String DEVICE_TYPE_DCDC = "DCDC";
    public static final String DEVICE_TYPE_MPPT = "MPPT";
    public static final String DEVICE_TYPE_BMS = "BMS";
    public static final String DEVICE_TYPE_AC_BUS = "AC_BUS";
    public static final String DEVICE_TYPE_DC_BUS = "DC_BUS";
    public static final String DEVICE_TYPE_AC_LOAD = "AC_LOAD";
    public static final String DEVICE_TYPE_DC_LOAD = "DC_LOAD";
    public static final String DEVICE_TYPE_METER_PREVENT_BACKFLOW = "METER_PREVENT_BACKFLOW";
    public static final String DEVICE_TYPE_METER_MEASURING = "METER_MEASURING";

    public static final String CATEGORY_POWER_CONVERTER = "POWER_CONVERTER";
    public static final String CATEGORY_BATTERY_MANAGEMENT = "BATTERY_MANAGEMENT";
    public static final String CATEGORY_BUS = "BUS";
    public static final String CATEGORY_LOAD = "LOAD";
    public static final String CATEGORY_METER = "METER";

    public static final String COMMUNICATION_MODBUS_TCP = "MODBUS_TCP";
    public static final String COMMUNICATION_MODBUS_RTU = "MODBUS_RTU";

    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_FAULT = 1;
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_CHARGING = 1;
    public static final int STATUS_DISCHARGING = 1;
    public static final int STATUS_STANDBY = 1;

    public static final double DEFAULT_AC_VOLTAGE = 380.0;
    public static final double DEFAULT_AC_FREQUENCY = 50.0;
    public static final double DEFAULT_DC_VOLTAGE = 800.0;

    public static final double POWER_TOLERANCE = 0.01;
    public static final int MAX_ITERATIONS = 100;

    public static final double MIN_FREQUENCY = 45.0;
    public static final double MAX_FREQUENCY = 55.0;

    public static final double MIN_AC_VOLTAGE = 0.0;
    public static final double MAX_AC_VOLTAGE = 500.0;

    public static final double MIN_DC_VOLTAGE = 0.0;
    public static final double MAX_DC_VOLTAGE = 2000.0;

    private DeviceConstants() {
    }
}