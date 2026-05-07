package com.nessaj.ersim.engine;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.MqttPublisherService;
import com.nessaj.ersim.service.RuntimeStateService;
import com.nessaj.ersim.service.WebSocketPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class PowerSimulationEngine {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;
    private final WebSocketPublisherService publisherService;
    private final MqttPublisherService mqttPublisherService;
    private List<Point> cachedPoints = new ArrayList<>();
    private long lastPointCacheTime = 0;
    private static final long POINT_CACHE_INTERVAL_MS = 5000;

    private SystemState systemState = SystemState.GRID_CONNECTED;
    private String currentSubState = "STANDBY";

    private final Map<String, Double> busPowerMap = new HashMap<>();
    private final Map<String, Double> busVoltageMap = new HashMap<>();
    private final Map<String, Double> busCurrentMap = new HashMap<>();
    private final Random random = new Random();
    private long secondsOfDay;
    private double simulationTime = 0;
    private boolean initialized = false;

    private double ppv = 0;
    private double pdcL = 0;
    private double pacL = 0;
    private double soc = 69;
    private double pbatSet = 0;
    private double ppcs = 0;
    private double pbatActual = 0;
    private double vdc = 750;
    private double gridPower = 0;

    private double batteryCapacity = 1000;
    private double maxBatPower = 50;
    private double minSoc = 30;
    private double maxSoc = 95;

    private boolean pbatManualMode = false;
    private double pbatManualSetpoint = 0;

    private double irradiance = 0;
    private double cloudCover = 0;
    private double ambientTemp = 25;
    private String weatherCondition = "CLEAR";

    public PowerSimulationEngine(ConfigurationLoader configLoader, RuntimeStateService runtimeStateService,
                                 WebSocketPublisherService publisherService,
                                 MqttPublisherService mqttPublisherService) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
        this.publisherService = publisherService;
        this.mqttPublisherService = mqttPublisherService;
    }

    @PostConstruct
    public void init() {
        LocalTime now = LocalTime.now();
//        double currentHour = now.getHour() + now.getMinute() / 60.0 + now.getSecond() / 3600.0;
//        this.simulationTime = currentHour * 3600;
        log.info("PowerSimulationEngine initialized - current server time: {}, simulation time starts at: {} seconds", now, this.simulationTime);
    }

    /**
     * 每一帧 模拟逻辑处理
     *
     * @param deltaTimeSeconds
     */
    public void simulateStep(double deltaTimeSeconds) {
        simulationTime += deltaTimeSeconds;

        LocalTime now =LocalTime.now();
        secondsOfDay = now.toSecondOfDay();
        if (!initialized) {
            vdc = 750;
//            soc = 69;
            initialized = true;
            log.info("PowerSimulationEngine simulation state initialized - Vdc: {}V, SOC: {}%", vdc, soc);
        }

        updateWeather();
        updateInputs();
        calculateStateMachine();
        updateOutputs(deltaTimeSeconds);
        updateDeviceRuntimeData();
        publishRuntimeUpdates();
    }

    private void updateWeather() {
        double hour = (secondsOfDay % 86400) / 3600.0;
        double solarElevation = calculateSolarElevation(hour);

        if (solarElevation <= 0) {
            irradiance = 0;
            weatherCondition = "NIGHT";
            ambientTemp = 20 + random.nextDouble() * 5;
        } else {
            weatherCondition = random.nextDouble() > 0.7 ? "CLOUDY" : "CLEAR";
            cloudCover = weatherCondition.equals("CLOUDY") ? 0.4 + random.nextDouble() * 0.4 : random.nextDouble() * 0.2;

            double maxIrradiance = 1000 * solarElevation;
            irradiance = maxIrradiance * (1 - cloudCover) * (0.95 + random.nextDouble() * 0.1);

            ambientTemp = 25 + solarElevation * 10 * (1 - cloudCover * 0.5) + (random.nextDouble() - 0.5) * 3;
        }

        log.debug("Weather - Condition: {}, Irradiance: {}W/m2, CloudCover: {}%, Temp: {}°C, Hour: {}",
                weatherCondition, String.format("%.0f", irradiance),
                String.format("%.0f", cloudCover * 100),
                String.format("%.1f", ambientTemp), String.format("%.1f", hour));
    }

    private double calculateSolarElevation(double hour) {
        if (hour < 6 || hour > 18) return 0;
        double distFromNoon = Math.abs(hour - 12);
        return Math.cos(distFromNoon * Math.PI / 12);
    }

    private void updateInputs() {
        double hour = (secondsOfDay % 86400) / 3600.0;
        double solarElevation = calculateSolarElevation(hour);

        if (irradiance > 50) {
            ppv = 50 * (irradiance / 1000) * (0.95 + random.nextDouble() * 0.1);
        } else {
            ppv = 0;
        }

        double loadFactor = calculateLoadPattern(hour);
        pdcL = 30 * loadFactor * (0.9 + random.nextDouble() * 0.2);
        pacL = 30 * loadFactor * (0.9 + random.nextDouble() * 0.2);

        log.debug("Inputs - PPV: {}kW, PdcL: {}kW, PacL: {}kW, SOC: {}%",
                String.format("%.2f", ppv), String.format("%.2f", pdcL),
                String.format("%.2f", pacL), String.format("%.1f", soc));
    }

    private void calculateStateMachine() {
        if (systemState == SystemState.GRID_CONNECTED) {
            calculateGridConnectedState();
        } else {
            calculateOffGridState();
        }
    }

    private void calculateGridConnectedState() {
        // 在 calculateGridConnectedState() 方法开头添加
        double hour = (secondsOfDay % 86400) / 3600.0;
        if (pbatManualMode) {
            currentSubState = "MANUAL_SET";
            pbatSet = pbatManualSetpoint;
            pbatSet = clampBySoc(pbatSet);
        } else {
            boolean pvLarge = ppv > 35;
            boolean pvSmall = ppv < 10;
            boolean socNormal = soc >= minSoc && soc <= maxSoc;
            boolean socLow = soc < minSoc;
            boolean socFull = soc >= maxSoc;
            boolean dcHeavy = pdcL > 50;
            boolean dcMedium = pdcL > 20 && pdcL <= 50;

            double excessPower = ppv - pdcL;

            // ========== 新增你的逻辑 ==========
            boolean isDaytime = hour >= 6 && hour <= 18;
            boolean isNight = !isDaytime;
            boolean pvInsufficient = ppv < pdcL;  // 光伏不够
            boolean socHigh = soc > 80;            // SOC > 80%
            boolean socAboveMin = soc > 20;        // SOC > 20%

            // 白天：光伏不够且SOC>80% → BMS放电
            if (isDaytime && pvInsufficient && socHigh) {
                currentSubState = "DAY_PV_INSUFFICIENT_BAT_DISCHARGE";
                pbatSet = Math.min(pdcL - ppv, maxBatPower * 0.8);  // 放电补充差额
                pbatSet = clamp(pbatSet, 0, maxBatPower);
            }
            // 晚上：SOC>20% → BMS放电
            else if (isNight && pvInsufficient && socAboveMin) {
                currentSubState = "NIGHT_BAT_DISCHARGE";
                pbatSet = Math.min(pdcL + pacL - ppv, maxBatPower * 0.6);
                pbatSet = clamp(pbatSet, 0, maxBatPower);
            } else if (excessPower > 0 && !socFull) {
                currentSubState = "PV_EXCESS_TO_BAT";
                double chargePower = Math.min(excessPower, maxBatPower * 0.8);
                double targetSoc = maxSoc;
                if (soc + chargePower * 0.1 > targetSoc) {
                    chargePower = (targetSoc - soc) * 10;
                }
                pbatSet = -chargePower;
            } else if (excessPower > 0 && socFull) {
                currentSubState = "PV_EXCESS_TO_GRID";
                pbatSet = 0;
            } else if (pvLarge && socNormal && dcHeavy) {
                currentSubState = "PV_BAT_SUPPLY";
                pbatSet = pdcL - ppv;
                pbatSet = clamp(pbatSet, -maxBatPower, maxBatPower);
            } else if (pvLarge && socNormal && (dcMedium || !dcHeavy)) {
                currentSubState = "TRANSFORMER_UPGRADE";
                pbatSet = maxBatPower * 0.3;
                pbatSet = clamp(pbatSet, 0, maxBatPower);
            } else if (pvSmall && socNormal && dcHeavy) {
                currentSubState = "PV_LOW_COMPENSATE";
                pbatSet = Math.min(pdcL - ppv, maxBatPower * 0.5);
                pbatSet = clamp(pbatSet, 0, maxBatPower);
            } else if (ppv < 5 && socLow && dcHeavy) {
                currentSubState = "BAT_CHARGING";
                pbatSet = -maxBatPower * 0.3;
            } else if (ppv > 15 && ppv < 35 && soc < maxSoc && dcMedium) {
                currentSubState = "BAT_NOT_FULL_CHARGING";
                pbatSet = -(ppv - pdcL) * 0.3;
                pbatSet = clamp(pbatSet, -maxBatPower, 0);
            } else if (ppv > 15 && ppv < 35 && socFull && dcMedium) {
                currentSubState = "BAT_FULL_NOT_CHARGING";
                pbatSet = 0;
            } else {
                currentSubState = "STANDBY";
                pbatSet = 0;
            }

            pbatSet = clampBySoc(pbatSet);
        }

        ppcs = ppv + pbatSet - pdcL;
        ppcs = clamp(ppcs, -125, 125);
        pbatActual = pbatSet;

        gridPower = -ppcs;
    }

    private void calculateOffGridState() {
        double hour = (secondsOfDay  % 86400) / 3600.0;
        double solarElevation = calculateSolarElevation(hour);
        boolean pvActive = solarElevation > 0.1;
        boolean pvLarge = irradiance > 600;
        boolean pvSmall = irradiance < 200;
        boolean socSufficient = soc > minSoc;
        boolean loadHeavy = (pdcL + pacL) > 60;
        boolean loadLight = (pdcL + pacL) < 30;

        if (pbatManualMode) {
            currentSubState = "MANUAL_SET";
            pbatSet = pbatManualSetpoint;
            pbatSet = clampBySoc(pbatSet);
        } else if (pvLarge && soc < maxSoc && loadLight) {
            currentSubState = "PV_MORE_BAT_CHARGE";
            pbatSet = -(maxBatPower * 0.4);
            vdc = clamp(vdc + 2, 700, 800);
        } else if ((pvSmall || !pvActive) && socSufficient && loadHeavy) {
            currentSubState = "PV_LESS_BAT_DISCHARGE";
            pbatSet = (pdcL + pacL - ppv) * 0.6;
            pbatSet = clamp(pbatSet, 0, maxBatPower);
            vdc = clamp(vdc - 3, 700, 800);
        } else if (ppv > (pdcL + pacL) * 0.8 && pvActive) {
            currentSubState = "PV_MORE_BAT_CHARGE";
            pbatSet = -(ppv - pdcL - pacL) * 0.4;
            pbatSet = clamp(pbatSet, -maxBatPower, 0);
            vdc = clamp(vdc + 1, 700, 800);
        } else {
            currentSubState = "PV_LESS_BAT_DISCHARGE";
            pbatSet = (pdcL + pacL - ppv) * 0.5;
            pbatSet = clamp(pbatSet, 0, maxBatPower);
            vdc = clamp(vdc - 1, 700, 800);
        }

        pbatSet = clampBySoc(pbatSet);
        pbatActual = pbatSet;
        ppcs = 0;
        gridPower = 0;

        double powerBalance = ppv + pbatActual - pdcL - pacL;
        vdc += powerBalance * 0.1;
        vdc = clamp(vdc, 700, 800);
    }

    private double clampBySoc(double power) {
        if (soc >= maxSoc && power < 0) {
            log.debug("SOC {} >= {}%, prevent charging", soc, maxSoc);
            return 0;
        }
        if (soc <= minSoc && power > 0) {
            log.debug("SOC {} <= {}%, prevent discharging", soc, minSoc);
            return 0;
        }
        if (soc >= maxSoc - 5 && power < 0) {
            log.debug("SOC {} near max, limit charging", soc);
            return Math.min(0, power * 0.3);
        }
        if (soc <= minSoc + 5 && power > 0) {
            log.debug("SOC {} near min, limit discharging", soc);
            return Math.max(0, power * 0.3);
        }
        return power;
    }

    private void updateOutputs(double deltaTimeSeconds) {
        double hoursElapsed = deltaTimeSeconds / 3.6;
        double deltaSoc = -(pbatActual / batteryCapacity) * hoursElapsed * 10;
        soc = clamp(soc + deltaSoc, 5, 100);
        soc = Math.round(soc * 1000) / 1000.0;

        log.debug("Outputs - Pbat_set: {}kW, Ppcs: {}kW, Pbat_actual: {}kW, Vdc: {}V, Grid: {}kW, SOC: {}%",
                String.format("%.2f", pbatSet), String.format("%.2f", ppcs),
                String.format("%.2f", pbatActual), String.format("%.2f", vdc),
                String.format("%.2f", gridPower), String.format("%.1f", soc));
    }

    private void updateDeviceRuntimeData() {
        log.trace("updateDeviceRuntimeData called");
        updateMpptData();
        updateBmsData();
        updateDcdcData();
        updatePcsData();
        updateAcLoadData();
        updateDcLoadData();
        updateMeterData();
        updateBusData();
    }

    private void updateMpptData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("mppt_01");
        if (data == null) return;

        boolean isRunning = irradiance > 50;
        data.setTelesignalValue("running", isRunning ? 1 : 0);
        data.setTelesignalValue("ready", !isRunning ? 1 : 0);

        if (!isRunning) {
            for (int i = 1; i <= 4; i++) {
                data.setTelemetryValue("pv" + i + "Power", 0.0);
                data.setTelemetryValue("pv" + i + "Voltage", 0.0);
                data.setTelemetryValue("pv" + i + "Current", 0.0);
            }
            data.setTelemetryValue("hvVoltage", vdc);
            data.setTelemetryValue("moduleTemp", ambientTemp);
            return;
        }

        double[] pvPowers = new double[4];
        for (int i = 0; i < 4; i++) {
            pvPowers[i] = (ppv / 4) * (0.9 + random.nextDouble() * 0.2);
            double voltage = 600 + random.nextDouble() * 150;
            // 注意单位是kw，还要乘1000
            double current = pvPowers[i] * 1000 / voltage;
            data.setTelemetryValue("pv" + (i + 1) + "Power", pvPowers[i]);
            data.setTelemetryValue("pv" + (i + 1) + "Voltage", voltage);
            data.setTelemetryValue("pv" + (i + 1) + "Current", current);
        }

        data.setTelemetryValue("hvVoltage", vdc);
        double temp = ambientTemp + irradiance * 0.02 + (random.nextDouble() - 0.5) * 2;
        data.setTelemetryValue("moduleTemp", temp);
    }

    private void updateBmsData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("bms_01");
        if (data == null) return;

        data.setTelemetryValue("totalSOC", soc);
        data.setTelemetryValue("avgSOC", soc);
        data.setTelemetryValue("minSOC", Math.max(5, soc - 3));
        data.setTelemetryValue("maxSOC", Math.min(100, soc + 3));

        double clusterVoltage = vdc * (0.98 + random.nextDouble() * 0.04);
        data.setTelemetryValue("clusterVoltage", clusterVoltage);
        data.setTelemetryValue("clusterCurrent", pbatActual * 1000 / clusterVoltage);

        data.setTelemetryValue("totalSOE", batteryCapacity * soc / 100);
        data.setTelemetryValue("avgCellVoltage", clusterVoltage / 16 * 1000);
        data.setTelemetryValue("maxCellVoltage", clusterVoltage / 16 * 1000 * 1.02);
        data.setTelemetryValue("minCellVoltage", clusterVoltage / 16 * 1000 * 0.98);

        double tempDelta = Math.abs(pbatActual) * 0.02;
        double maxTemp = ambientTemp + tempDelta + (random.nextDouble() - 0.5);
        data.setTelemetryValue("maxTemp", maxTemp);
        data.setTelemetryValue("minTemp", maxTemp - 3);
        data.setTelemetryValue("avgTemp", maxTemp - 1.5);

        double maxChargeCurrent = soc < 90 ? 350 : 350 * (100 - soc) / 20;
        double maxDischargeCurrent = soc > minSoc ? 350 : 350 * soc / 15;
        data.setTelemetryValue("maxChargeCurrent", maxChargeCurrent);
        data.setTelemetryValue("maxDischargeCurrent", maxDischargeCurrent);
        data.setTelemetryValue("maxChargePower", vdc * maxChargeCurrent / 1000);
        data.setTelemetryValue("maxDischargePower", vdc * maxDischargeCurrent / 1000);

        int batStatus = pbatActual > 1 ? 2 : (pbatActual < -1 ? 1 : 0);
        data.setTelemetryValue("batteryStatus", batStatus);
    }

    private void updateDcdcData() {
        boolean discharging = pbatActual > 1;
        boolean charging = pbatActual < -1;

        DeviceRuntimeData dcdc1 = runtimeStateService.getRuntimeData("dcdc_01");
        if (dcdc1 != null) {
            dcdc1.setTelemetryValue("hvVoltage", vdc);
            dcdc1.setTelemetryValue("batteryVoltage", vdc * 0.6);
            dcdc1.setTelemetryValue("batteryPower", pbatActual);
            dcdc1.setTelemetryValue("batteryCurrent", vdc > 0 ? pbatActual * 1000 / vdc : 0);
            dcdc1.setTelesignalValue("running", (discharging || charging) ? 1 : 0);
            dcdc1.setTelesignalValue("ready", (!discharging && !charging) ? 1 : 0);
            dcdc1.setTelesignalValue("selfCheck", 0);
            dcdc1.setTelesignalValue("initializing", 0);
            double temp = ambientTemp + Math.abs(pbatActual) * 0.05 + (random.nextDouble() - 0.5);
            dcdc1.setTelemetryValue("moduleTemp", temp);
        }

        DeviceRuntimeData dcdc2 = runtimeStateService.getRuntimeData("dcdc_02");
        if (dcdc2 != null) {
            dcdc2.setTelemetryValue("hvVoltage", vdc);
            dcdc2.setTelemetryValue("batteryVoltage", vdc * 0.4);
            dcdc2.setTelemetryValue("batteryPower", pbatActual);
            dcdc2.setTelemetryValue("batteryCurrent", vdc > 0 ? pbatActual * 1000 / vdc : 0);
            dcdc2.setTelesignalValue("running", (discharging || charging) ? 1 : 0);
            dcdc2.setTelesignalValue("ready", (!discharging && !charging) ? 1 : 0);
            dcdc2.setTelesignalValue("selfCheck", 0);
            dcdc2.setTelesignalValue("initializing", 0);
            double temp = ambientTemp + Math.abs(pbatActual) * 0.05 + (random.nextDouble() - 0.5);
            dcdc2.setTelemetryValue("moduleTemp", temp);
        }
    }

    private void updatePcsData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("pcs_01");
        if (data == null) return;

        data.setTelemetryValue("activePower", -ppcs);
        data.setTelemetryValue("reactivePower", 0);
        data.setTelemetryValue("dcPower", -ppcs);
        data.setTelemetryValue("dcVoltage", vdc);
        data.setTelemetryValue("acVoltageA", 220 + (random.nextDouble() - 0.5) * 10);
        data.setTelemetryValue("acVoltageB", 220 + (random.nextDouble() - 0.5) * 10);
        data.setTelemetryValue("acVoltageC", 220 + (random.nextDouble() - 0.5) * 10);
        data.setTelemetryValue("acFrequency", 50 + (random.nextDouble() - 0.5) * 0.2);

        double pf = 1.0;
        data.setTelemetryValue("powerFactor", pf);
        data.setTelemetryValue("apparentPower", Math.abs(ppcs));

        boolean gridConnected = systemState == SystemState.GRID_CONNECTED;
        data.setTelesignalValue("gridConnected", gridConnected ? 1 : 0);
        data.setTelesignalValue("standby", Math.abs(ppcs) < 1 ? 1 : 0);
        data.setTelesignalValue("running", Math.abs(ppcs) >= 1 ? 1 : 0);
        data.setTelesignalValue("charging", ppcs < -1 ? 1 : 0);
        data.setTelesignalValue("discharging", ppcs > 1 ? 1 : 0);
        data.setTelesignalValue("alarm", 0);
        data.setTelesignalValue("fault", 0);
        data.setTelesignalValue("starting", 0);
        data.setTelesignalValue("derating", 0);

        double temp = 35 + Math.abs(ppcs) * 0.05 + (random.nextDouble() - 0.5);
        data.setTelemetryValue("igbtTempA", temp);
        data.setTelemetryValue("igbtTempB", temp + 1);
        data.setTelemetryValue("igbtTempC", temp - 1);
        data.setTelemetryValue("envTemp", ambientTemp);
    }

    private void updateAcLoadData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("ac_load_01");
        if (data == null) return;

        data.setTelemetryValue("activePower", pacL);
        data.setTelemetryValue("reactivePower", pacL * 0.3);
        data.setTelemetryValue("apparentPower", pacL * 1.1);
        data.setTelemetryValue("voltage", 380 + (random.nextDouble() - 0.5) * 10);
        data.setTelemetryValue("current", pacL * 1000 / 380 / 1.732);
        data.setTelemetryValue("powerFactor", 0.9);
        data.setTelesignalValue("enabled", 1);
    }

    private void updateDcLoadData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("dc_load_01");
        if (data == null) return;

        data.setTelemetryValue("activePower", pdcL);
        data.setTelemetryValue("voltage", vdc);
        data.setTelemetryValue("current", vdc > 0 ? pdcL * 1000 / vdc : 0);
        data.setTelesignalValue("enabled", 1);
    }

    private void updateMeterData() {
        DeviceRuntimeData data = runtimeStateService.getRuntimeData("meter_01");
        if (data == null) return;

        data.setTelemetryValue("instantPower", gridPower);
        data.setTelemetryValue("voltage", 380);
        data.setTelemetryValue("current", Math.abs(gridPower) * 1000 / 380 / 1.732);
        data.setTelemetryValue("forwardEnergy", 0);
        data.setTelemetryValue("reverseEnergy", 0);

        data.setTelesignalValue("reversePower", gridPower < 0 ? 1 : 0);
        data.setTelesignalValue("alarm", 0);
    }

    private void updateBusData() {
        DeviceRuntimeData busAc = runtimeStateService.getRuntimeData("bus_ac");
        if (busAc != null) {
            busAc.setTelemetryValue("voltage", 380);
            busAc.setTelemetryValue("current", (pacL + Math.abs(gridPower)) * 1000 / 380 / 1.732);
            busAc.setTelemetryValue("activePower", pacL + Math.abs(gridPower));
        }

        DeviceRuntimeData busDc = runtimeStateService.getRuntimeData("bus_dc");
        if (busDc != null) {
            busDc.setTelemetryValue("voltage", vdc);
            double dcCurrent = (ppv + Math.abs(pbatActual) + pdcL) * 1000 / vdc;
            busDc.setTelemetryValue("current", dcCurrent);
            busDc.setTelemetryValue("activePower", ppv + Math.abs(pbatActual) - pdcL);
        }

        busVoltageMap.put("bus_ac", 380.0);
        busVoltageMap.put("bus_dc", vdc);
        busCurrentMap.put("bus_ac", busAc != null ? (Double) busAc.getTelemetryValue("current") : 0);
        busCurrentMap.put("bus_dc", busDc != null ? (Double) busDc.getTelemetryValue("current") : 0);
        busPowerMap.put("bus_ac", pacL + Math.abs(gridPower));
        busPowerMap.put("bus_dc", ppv + Math.abs(pbatActual) - pdcL);
    }

    private double calculateLoadPattern(double hour) {
        double base = 0.6;
        if (hour >= 7 && hour <= 11) base = 0.8;
        if (hour >= 12 && hour <= 14) base = 0.7;
        if (hour >= 17 && hour <= 21) base = 0.9;
        if (hour >= 22 || hour <= 6) base = 0.4;
        return base * (0.9 + random.nextDouble() * 0.2);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void publishRuntimeUpdates() {
        Map<String, DeviceRuntimeData> allRuntimeData = runtimeStateService.getAllRuntimeData();
        log.debug("Publishing runtime updates, device count: {}", allRuntimeData.size());

        Map<String, Object> deviceUpdates = new HashMap<>();
        for (Map.Entry<String, DeviceRuntimeData> entry : allRuntimeData.entrySet()) {
            log.trace("Publishing device {}: telemetry={}", entry.getKey(), entry.getValue().getTelemetry());
            deviceUpdates.put(entry.getKey(), entry.getValue().toMap());
        }

        Map<String, Object> updateMessage = new HashMap<>();
        updateMessage.put("devices", deviceUpdates);
        updateMessage.put("busPowers", busPowerMap);
        updateMessage.put("busVoltages", busVoltageMap);
        updateMessage.put("busCurrents", busCurrentMap);
        updateMessage.put("simulationTime", simulationTime);
        updateMessage.put("systemState", systemState.name());
        updateMessage.put("subState", currentSubState);
        updateMessage.put("pbatManualMode", pbatManualMode);
        updateMessage.put("pbatManualSetpoint", pbatManualSetpoint);
        updateMessage.put("weather", Map.of(
                "condition", weatherCondition,
                "irradiance", irradiance,
                "cloudCover", cloudCover,
                "ambientTemp", ambientTemp
        ));
        updateMessage.put("inputs", Map.of("ppv", ppv, "pdcL", pdcL, "pacL", pacL, "soc", soc));
        updateMessage.put("outputs", Map.of("pbatSet", pbatSet, "ppcs", ppcs, "pbatActual", pbatActual, "vdc", vdc, "gridPower", gridPower));
        updateMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        publisherService.publishDeviceUpdate(updateMessage);
        publisherService.publishSimulationUpdate(Map.of(
                "simulationTime", simulationTime,
                "systemState", systemState.name(),
                "subState", currentSubState,
                "weather", Map.of(
                        "condition", weatherCondition,
                        "irradiance", irradiance,
                        "cloudCover", cloudCover,
                        "ambientTemp", ambientTemp
                ),
                "inputs", Map.of("ppv", ppv, "pdcL", pdcL, "pacL", pacL, "soc", soc),
                "outputs", Map.of("pbatSet", pbatSet, "ppcs", ppcs, "pbatActual", pbatActual, "vdc", vdc, "gridPower", gridPower),
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));

        long now = System.currentTimeMillis();
        if (cachedPoints.isEmpty() || now - lastPointCacheTime > POINT_CACHE_INTERVAL_MS) {
            cachedPoints = configLoader.getAllPoints();
            lastPointCacheTime = now;
        }
        mqttPublisherService.publishSimulationData(allRuntimeData, cachedPoints);

        // 推送MQTT状态到WebSocket
        MqttState mqttState = mqttPublisherService.getMqttState();
        publisherService.publishMqttState(mqttState);
    }

    public void setSystemState(SystemState state) {
        this.systemState = state;
        log.info("System state changed to: {}", state.name());
    }

    public SystemState getSystemState() {
        return systemState;
    }

    public String getCurrentSubState() {
        return currentSubState;
    }

    public void setPbatManualMode(boolean manual, double setpoint) {
        this.pbatManualMode = manual;
        this.pbatManualSetpoint = clamp(setpoint, -maxBatPower, maxBatPower);
        log.info("Pbat manual mode: {}, setpoint: {}kW", manual, this.pbatManualSetpoint);
    }

    public boolean isPbatManualMode() {
        return pbatManualMode;
    }

    public double getPbatManualSetpoint() {
        return pbatManualSetpoint;
    }
}