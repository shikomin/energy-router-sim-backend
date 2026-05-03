package com.nessaj.ersim.engine;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.model.*;
import com.nessaj.ersim.service.RuntimeStateService;
import com.nessaj.ersim.service.WebSocketPublisherService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PowerSimulationEngine {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;
    private final WebSocketPublisherService publisherService;
    private final Map<String, Double> busPowerMap;
    private final Map<String, Double> busVoltageMap;
    private final Map<String, Double> busCurrentMap;
    private final Random random;
    private double simulationTime;

    public PowerSimulationEngine(ConfigurationLoader configLoader, RuntimeStateService runtimeStateService,
                                 WebSocketPublisherService publisherService) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
        this.publisherService = publisherService;
        this.busPowerMap = new HashMap<>();
        this.busVoltageMap = new HashMap<>();
        this.busCurrentMap = new HashMap<>();
        this.random = new Random();
        this.simulationTime = 0.0;
    }

    public void simulateStep(double deltaTimeSeconds) {
        simulationTime += deltaTimeSeconds;
        
        List<Topology> topologies = configLoader.getAllTopologies();

        for (Topology topology : topologies) {
            simulateTopologyStep(topology, deltaTimeSeconds);
        }
        
        publishRuntimeUpdates();
    }

    private void publishRuntimeUpdates() {
        Map<String, DeviceRuntimeData> allRuntimeData = runtimeStateService.getAllRuntimeData();
        
        Map<String, Object> deviceUpdates = new HashMap<>();
        for (Map.Entry<String, DeviceRuntimeData> entry : allRuntimeData.entrySet()) {
            deviceUpdates.put(entry.getKey(), entry.getValue().toMap());
        }
        
        Map<String, Object> updateMessage = new HashMap<>();
        updateMessage.put("devices", deviceUpdates);
        updateMessage.put("busPowers", busPowerMap);
        updateMessage.put("busVoltages", busVoltageMap);
        updateMessage.put("busCurrents", busCurrentMap);
        updateMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        publisherService.publishDeviceUpdate(updateMessage);
    }

    private void simulateTopologyStep(Topology topology, double deltaTimeSeconds) {
        initializeDeviceData(topology);
        
        Map<String, Double> busPowerBalances = calculateBusPowerBalances(topology);
        
        for (Map.Entry<String, Double> entry : busPowerBalances.entrySet()) {
            String busId = entry.getKey();
            Double powerBalance = entry.getValue();
            
            Bus bus = topology.getBus(busId);
            if (bus == null) continue;
            
            updateBusState(bus, powerBalance);
            
            for (String deviceId : bus.getConnectedDevices()) {
                Device device = topology.getDevice(deviceId);
                if (device == null) continue;
                
                DeviceRuntimeData runtimeData = runtimeStateService.getRuntimeData(deviceId);
                if (runtimeData == null) continue;
                
                simulateDevice(device, runtimeData, powerBalance, deltaTimeSeconds);
            }
            
            adjustPowerBalance(topology, bus, powerBalance);
        }
    }

    private void initializeDeviceData(Topology topology) {
        for (Device device : topology.getDevices().values()) {
            DeviceRuntimeData runtimeData = runtimeStateService.getRuntimeData(device.getId());
            if (runtimeData == null) {
                runtimeData = new DeviceRuntimeData(device.getId());
                initializeDeviceRuntimeData(device, runtimeData);
                runtimeStateService.addRuntimeData(device.getId(), runtimeData);
            }
        }
    }

    private void initializeDeviceRuntimeData(Device device, DeviceRuntimeData runtimeData) {
        switch (device.getType()) {
            case "PCS":
                runtimeData.setTelemetryValue("activePower", 0.0);
                runtimeData.setTelemetryValue("reactivePower", 0.0);
                runtimeData.setTelemetryValue("apparentPower", 0.0);
                runtimeData.setTelemetryValue("dcPower", 0.0);
                runtimeData.setTelemetryValue("totalBusVoltage", 8000.0);
                runtimeData.setTelemetryValue("acVoltageAB", 3800.0);
                runtimeData.setTelemetryValue("acVoltageBC", 3800.0);
                runtimeData.setTelemetryValue("acVoltageCA", 3800.0);
                runtimeData.setTelemetryValue("acFrequency", 5000);
                runtimeData.setTelemetryValue("powerFactor", 1000);
                runtimeData.setTelesignalValue("running", 1);
                runtimeData.setTelesignalValue("standby", 0);
                runtimeData.setTeleadjustValue("activePowerSet", 0.0);
                break;
                
            case "MPPT":
                runtimeData.setTelemetryValue("pv1Power", 120.0);
                runtimeData.setTelemetryValue("pv2Power", 115.0);
                runtimeData.setTelemetryValue("pv3Power", 125.0);
                runtimeData.setTelemetryValue("pv4Power", 110.0);
                runtimeData.setTelemetryValue("hvVoltage", 8000.0);
                runtimeData.setTelemetryValue("moduleTemp", 350.0);
                runtimeData.setTelesignalValue("running", 1);
                break;
                
            case "DCDC":
                runtimeData.setTelemetryValue("batteryPower", 0.0);
                runtimeData.setTelemetryValue("batteryVoltage", 4800.0);
                runtimeData.setTelemetryValue("batteryCurrent", 0.0);
                runtimeData.setTelemetryValue("hvVoltage", 8000.0);
                runtimeData.setTelemetryValue("moduleTemp", 250.0);
                runtimeData.setTelesignalValue("running", 1);
                runtimeData.setTeleadjustValue("batteryPowerSet", 0.0);
                break;
                
            case "BMS":
                runtimeData.setTelemetryValue("clusterVoltage", 5120.0);
                runtimeData.setTelemetryValue("clusterCurrent", 0.0);
                runtimeData.setTelemetryValue("totalSOC", 500.0);
                runtimeData.setTelemetryValue("totalSOH", 100.0);
                runtimeData.setTelemetryValue("maxTemp", 250.0);
                runtimeData.setTelemetryValue("minTemp", 250.0);
                runtimeData.setTelemetryValue("avgTemp", 250.0);
                runtimeData.setTelemetryValue("maxChargeCurrent", 5000.0);
                runtimeData.setTelemetryValue("maxDischargeCurrent", 5000.0);
                break;
                
            case "AC_LOAD":
                runtimeData.setTelemetryValue("activePower", -500.0);
                runtimeData.setTelemetryValue("reactivePower", -100.0);
                runtimeData.setTelemetryValue("voltage", 3800.0);
                runtimeData.setTelemetryValue("current", 750.0);
                runtimeData.setTeleadjustValue("activePowerSet", 500.0);
                break;
                
            case "DC_LOAD":
                runtimeData.setTelemetryValue("activePower", -300.0);
                runtimeData.setTelemetryValue("voltage", 8000.0);
                runtimeData.setTelemetryValue("current", 375.0);
                runtimeData.setTeleadjustValue("activePowerSet", 300.0);
                break;
                
            case "METER_PREVENT_BACKFLOW":
                runtimeData.setTelemetryValue("instantPower", 0.0);
                runtimeData.setTelemetryValue("forwardEnergy", 1000.0);
                runtimeData.setTelemetryValue("reverseEnergy", 0.0);
                runtimeData.setTelemetryValue("voltage", 3800.0);
                break;
        }
    }

    private Map<String, Double> calculateBusPowerBalances(Topology topology) {
        Map<String, Double> busPowers = new HashMap<>();

        for (Bus bus : topology.getBuses().values()) {
            double totalActivePower = 0.0;
            double totalReactivePower = 0.0;

            for (String deviceId : bus.getConnectedDevices()) {
                Device device = topology.getDevice(deviceId);
                if (device == null) continue;

                DeviceRuntimeData runtimeData = runtimeStateService.getRuntimeData(deviceId);
                if (runtimeData == null) continue;

                double deviceActivePower = getDeviceActivePower(device.getType(), runtimeData);
                double deviceReactivePower = getDeviceReactivePower(device.getType(), runtimeData);

                totalActivePower += deviceActivePower;
                totalReactivePower += deviceReactivePower;
            }

            bus.setActivePower(totalActivePower);
            bus.setReactivePower(totalReactivePower);
            busPowerMap.put(bus.getId(), totalActivePower);
        }

        return busPowerMap;
    }

    private double getDeviceActivePower(String deviceType, DeviceRuntimeData runtimeData) {
        switch (deviceType) {
            case "PCS":
                return getDoubleValue(runtimeData.getTelemetryValue("activePower")) / 10.0;
            case "MPPT":
                double mpptPower = 0;
                mpptPower += getDoubleValue(runtimeData.getTelemetryValue("pv1Power")) / 10.0;
                mpptPower += getDoubleValue(runtimeData.getTelemetryValue("pv2Power")) / 10.0;
                mpptPower += getDoubleValue(runtimeData.getTelemetryValue("pv3Power")) / 10.0;
                mpptPower += getDoubleValue(runtimeData.getTelemetryValue("pv4Power")) / 10.0;
                return mpptPower;
            case "DCDC":
                return getDoubleValue(runtimeData.getTelemetryValue("batteryPower")) / 10.0;
            case "AC_LOAD":
            case "DC_LOAD":
                return -Math.abs(getDoubleValue(runtimeData.getTelemetryValue("activePower")) / 10.0);
            case "METER_PREVENT_BACKFLOW":
                return getDoubleValue(runtimeData.getTelemetryValue("instantPower")) / 10.0;
            default:
                return 0.0;
        }
    }

    private double getDeviceReactivePower(String deviceType, DeviceRuntimeData runtimeData) {
        switch (deviceType) {
            case "PCS":
                return getDoubleValue(runtimeData.getTelemetryValue("reactivePower")) / 10.0;
            case "AC_LOAD":
                return -Math.abs(getDoubleValue(runtimeData.getTelemetryValue("reactivePower")) / 10.0);
            default:
                return 0.0;
        }
    }

    private void updateBusState(Bus bus, double powerBalance) {
        double voltage = bus.getRatedVoltage();
        double current = Math.abs(powerBalance) / voltage;
        
        bus.setVoltage(voltage);
        bus.setCurrent(current);
        busPowerMap.put(bus.getId(), powerBalance);
        busVoltageMap.put(bus.getId(), voltage);
        busCurrentMap.put(bus.getId(), current);
    }

    private void simulateDevice(Device device, DeviceRuntimeData runtimeData, double busPowerBalance, double deltaTimeSeconds) {
        switch (device.getType()) {
            case "PCS":
                simulatePcs(runtimeData, busPowerBalance, deltaTimeSeconds);
                break;
            case "DCDC":
                simulateDcdc(runtimeData, busPowerBalance, deltaTimeSeconds);
                break;
            case "MPPT":
                simulateMppt(runtimeData, deltaTimeSeconds);
                break;
            case "BMS":
                simulateBms(runtimeData, deltaTimeSeconds);
                break;
            case "AC_LOAD":
                simulateAcLoad(runtimeData, deltaTimeSeconds);
                break;
            case "DC_LOAD":
                simulateDcLoad(runtimeData, deltaTimeSeconds);
                break;
            case "METER_PREVENT_BACKFLOW":
                simulateMeter(runtimeData, deltaTimeSeconds);
                break;
        }
    }

    private void simulatePcs(DeviceRuntimeData runtimeData, double busPowerBalance, double deltaTimeSeconds) {
        Integer running = runtimeData.getTelesignalValue("running");
        if (running == null || running == 0) return;

        double gridPower = -busPowerBalance;
        double efficiency = 0.98;
        
        double targetPower = Math.max(-100.0, Math.min(100.0, gridPower));
        double currentPower = getDoubleValue(runtimeData.getTelemetryValue("activePower")) / 10.0;
        
        double newActivePower = currentPower + (targetPower - currentPower) * deltaTimeSeconds * 0.5;
        
        runtimeData.setTelemetryValue("activePower", newActivePower * 10);
        
        double dcPower = newActivePower / efficiency;
        runtimeData.setTelemetryValue("dcPower", dcPower * 10);
        
        double pf = getDoubleValue(runtimeData.getTeleadjustValue("powerFactorSet")) / 1000.0;
        if (pf == 0) pf = 1.0;
        double apparentPower = Math.abs(newActivePower) / pf;
        double reactivePower = Math.sqrt(apparentPower * apparentPower - newActivePower * newActivePower);
        
        if (newActivePower < 0) reactivePower = -reactivePower;
        runtimeData.setTelemetryValue("reactivePower", reactivePower * 10);
        runtimeData.setTelemetryValue("apparentPower", apparentPower * 10);
        runtimeData.setTelemetryValue("powerFactor", pf * 1000);
        
        double freq = 5000 + (random.nextDouble() - 0.5) * 10;
        runtimeData.setTelemetryValue("acFrequency", (int) freq);
        
        int charging = newActivePower < -0.1 ? 1 : 0;
        int discharging = newActivePower > 0.1 ? 1 : 0;
        runtimeData.setTelesignalValue("charging", charging);
        runtimeData.setTelesignalValue("discharging", discharging);
    }

    private void simulateDcdc(DeviceRuntimeData runtimeData, double busPowerBalance, double deltaTimeSeconds) {
        Integer running = runtimeData.getTelesignalValue("running");
        if (running == null || running == 0) return;

        double pcsDcPower = getDoubleValue(runtimeData.getTeleadjustValue("batteryPowerSet")) / 10.0;
        double efficiency = 0.95;
        
        double currentPower = getDoubleValue(runtimeData.getTelemetryValue("batteryPower")) / 10.0;
        double newPower = currentPower + (pcsDcPower - currentPower) * deltaTimeSeconds * 0.3;
        
        runtimeData.setTelemetryValue("batteryPower", newPower * 10);
        
        double batteryVoltage = getDoubleValue(runtimeData.getTelemetryValue("batteryVoltage")) / 10.0;
        double batteryCurrent = batteryVoltage > 0 ? newPower / batteryVoltage : 0;
        runtimeData.setTelemetryValue("batteryCurrent", batteryCurrent * 10);
        
        double hvVoltage = 800 + random.nextDouble() * 10;
        runtimeData.setTelemetryValue("hvVoltage", hvVoltage * 10);
        
        double temp = getDoubleValue(runtimeData.getTelemetryValue("moduleTemp")) / 10.0;
        temp += Math.abs(newPower) * 0.005 * deltaTimeSeconds;
        temp += (random.nextDouble() - 0.5) * 0.5;
        temp = Math.max(25, Math.min(55, temp));
        runtimeData.setTelemetryValue("moduleTemp", temp * 10);
    }

    private void simulateMppt(DeviceRuntimeData runtimeData, double deltaTimeSeconds) {
        Integer running = runtimeData.getTelesignalValue("running");
        if (running == null || running == 0) return;

        double timeOfDay = (simulationTime % 86400) / 3600.0;
        double irradiance = calculateIrradiance(timeOfDay);
        
        double[] pvPowers = new double[4];
        double totalPower = 0;
        
        for (int i = 0; i < 4; i++) {
            double basePower = 25.0 * irradiance;
            double fluctuation = (0.9 + random.nextDouble() * 0.2);
            pvPowers[i] = basePower * fluctuation;
            totalPower += pvPowers[i];
        }
        
        runtimeData.setTelemetryValue("pv1Power", pvPowers[0] * 10);
        runtimeData.setTelemetryValue("pv2Power", pvPowers[1] * 10);
        runtimeData.setTelemetryValue("pv3Power", pvPowers[2] * 10);
        runtimeData.setTelemetryValue("pv4Power", pvPowers[3] * 10);
        
        double[] voltages = {600 + random.nextDouble() * 50, 610 + random.nextDouble() * 50,
                            590 + random.nextDouble() * 50, 605 + random.nextDouble() * 50};
        double[] currents = new double[4];
        
        for (int i = 0; i < 4; i++) {
            currents[i] = pvPowers[i] / voltages[i];
            runtimeData.setTelemetryValue("pv" + (i + 1) + "Voltage", voltages[i] * 10);
            runtimeData.setTelemetryValue("pv" + (i + 1) + "Current", currents[i] * 10);
        }
        
        double hvVoltage = 800 + random.nextDouble() * 20;
        runtimeData.setTelemetryValue("hvVoltage", hvVoltage * 10);
        
        double temp = 25 + irradiance * 15 + (random.nextDouble() - 0.5) * 5;
        runtimeData.setTelemetryValue("moduleTemp", temp * 10);
    }

    private double calculateIrradiance(double hour) {
        if (hour < 6 || hour > 18) return 0.0;
        
        double midday = 12.0;
        double distanceFromMidday = Math.abs(hour - midday);
        double irradiance = Math.cos(distanceFromMidday * Math.PI / 12);
        
        double cloudCover = 0.3 + random.nextDouble() * 0.2;
        irradiance *= (1 - cloudCover);
        
        irradiance += (random.nextDouble() - 0.5) * 0.1;
        
        return Math.max(0, Math.min(1, irradiance));
    }

    private void simulateBms(DeviceRuntimeData runtimeData, double deltaTimeSeconds) {
        double voltage = getDoubleValue(runtimeData.getTelemetryValue("clusterVoltage")) / 10.0;
        double current = getDoubleValue(runtimeData.getTelemetryValue("clusterCurrent")) / 10.0;
        double soc = getDoubleValue(runtimeData.getTelemetryValue("totalSOC")) / 10.0;
        
        double power = voltage * current / 1000.0;
        
        double socChange = -(power / 50.0) * deltaTimeSeconds * 0.1;
        soc = Math.max(5, Math.min(100, soc + socChange));
        runtimeData.setTelemetryValue("totalSOC", soc * 10);
        
        double voltageChange = (soc - 50) * 0.5;
        voltage = 500 + voltageChange + (random.nextDouble() - 0.5) * 2;
        runtimeData.setTelemetryValue("clusterVoltage", voltage * 10);
        
        runtimeData.setTelemetryValue("clusterCurrent", current * 10);
        
        double avgCellVoltage = voltage * 1000 / 128;
        runtimeData.setTelemetryValue("avgCellVoltage", Math.round(avgCellVoltage * 1000));
        
        double maxTemp = getDoubleValue(runtimeData.getTelemetryValue("maxTemp")) / 10.0;
        double tempChange = Math.abs(power) * 0.01 * deltaTimeSeconds;
        maxTemp += tempChange + (random.nextDouble() - 0.5) * 0.5;
        maxTemp = Math.max(20, Math.min(50, maxTemp));
        runtimeData.setTelemetryValue("maxTemp", maxTemp * 10);
        
        double minTemp = maxTemp - 5 - random.nextDouble() * 5;
        runtimeData.setTelemetryValue("minTemp", minTemp * 10);
        runtimeData.setTelemetryValue("avgTemp", ((maxTemp + minTemp) / 2) * 10);
        
        double maxChargeCurrent = Math.min(500, soc < 90 ? 500 : (100 - soc) * 50);
        double maxDischargeCurrent = Math.min(500, soc > 10 ? 500 : soc * 50);
        runtimeData.setTelemetryValue("maxChargeCurrent", maxChargeCurrent * 10);
        runtimeData.setTelemetryValue("maxDischargeCurrent", maxDischargeCurrent * 10);
    }

    private void simulateAcLoad(DeviceRuntimeData runtimeData, double deltaTimeSeconds) {
        double targetPower = getDoubleValue(runtimeData.getTeleadjustValue("activePowerSet")) / 10.0;
        double currentPower = Math.abs(getDoubleValue(runtimeData.getTelemetryValue("activePower")) / 10.0);
        
        double loadPattern = 1.0;
        double hour = (simulationTime % 86400) / 3600.0;
        if (hour >= 8 && hour <= 18) loadPattern = 1.2;
        if (hour >= 19 && hour <= 22) loadPattern = 0.9;
        
        targetPower *= loadPattern;
        targetPower += (random.nextDouble() - 0.5) * 5;
        
        double newPower = currentPower + (targetPower - currentPower) * deltaTimeSeconds * 0.2;
        newPower = Math.max(10, newPower);
        
        runtimeData.setTelemetryValue("activePower", -newPower * 10);
        
        double voltage = getDoubleValue(runtimeData.getTelemetryValue("voltage")) / 10.0;
        double current = newPower * 1000 / voltage;
        runtimeData.setTelemetryValue("current", current * 10);
        
        double pf = 0.85 + random.nextDouble() * 0.1;
        double reactivePower = newPower * Math.tan(Math.acos(pf));
        runtimeData.setTelemetryValue("reactivePower", -reactivePower * 10);
        runtimeData.setTelemetryValue("powerFactor", pf * 1000);
    }

    private void simulateDcLoad(DeviceRuntimeData runtimeData, double deltaTimeSeconds) {
        double targetPower = getDoubleValue(runtimeData.getTeleadjustValue("activePowerSet")) / 10.0;
        double currentPower = Math.abs(getDoubleValue(runtimeData.getTelemetryValue("activePower")) / 10.0);
        
        double loadPattern = 1.0;
        double hour = (simulationTime % 86400) / 3600.0;
        if (hour >= 8 && hour <= 18) loadPattern = 1.5;
        
        targetPower *= loadPattern;
        targetPower += (random.nextDouble() - 0.5) * 10;
        
        double newPower = currentPower + (targetPower - currentPower) * deltaTimeSeconds * 0.2;
        newPower = Math.max(0, newPower);
        
        runtimeData.setTelemetryValue("activePower", -newPower * 10);
        
        double voltage = getDoubleValue(runtimeData.getTelemetryValue("voltage")) / 10.0;
        double current = newPower * 1000 / voltage;
        runtimeData.setTelemetryValue("current", current * 10);
    }

    private void simulateMeter(DeviceRuntimeData runtimeData, double deltaTimeSeconds) {
        double gridPower = getDoubleValue(runtimeData.getTelemetryValue("instantPower")) / 10.0;
        
        double forwardEnergy = getDoubleValue(runtimeData.getTelemetryValue("forwardEnergy"));
        double reverseEnergy = getDoubleValue(runtimeData.getTelemetryValue("reverseEnergy"));
        
        if (gridPower > 0.1) {
            reverseEnergy += gridPower * deltaTimeSeconds / 3600;
        } else if (gridPower < -0.1) {
            forwardEnergy += Math.abs(gridPower) * deltaTimeSeconds / 3600;
        }
        
        runtimeData.setTelemetryValue("forwardEnergy", forwardEnergy);
        runtimeData.setTelemetryValue("reverseEnergy", reverseEnergy);
        
        runtimeData.setTelesignalValue("reversePower", gridPower > 0.1 ? 1 : 0);
    }

    private void adjustPowerBalance(Topology topology, Bus bus, double powerBalance) {
        if (Math.abs(powerBalance) < 0.1) return;
        
        if ("AC_BUS".equals(bus.getType())) {
            adjustAcBusPower(topology, bus, powerBalance);
        } else if ("DC_BUS".equals(bus.getType())) {
            adjustDcBusPower(topology, bus, powerBalance);
        }
    }

    private void adjustAcBusPower(Topology topology, Bus bus, double powerBalance) {
        DeviceRuntimeData meterData = null;
        DeviceRuntimeData pcsData = null;
        
        for (String deviceId : bus.getConnectedDevices()) {
            Device device = topology.getDevice(deviceId);
            if (device == null) continue;
            
            if ("METER_PREVENT_BACKFLOW".equals(device.getType())) {
                meterData = runtimeStateService.getRuntimeData(deviceId);
            } else if ("PCS".equals(device.getType())) {
                pcsData = runtimeStateService.getRuntimeData(deviceId);
            }
        }
        
        if (meterData != null) {
            double meterPower = getDoubleValue(meterData.getTelemetryValue("instantPower")) / 10.0;
            double newMeterPower = meterPower + powerBalance * 0.1;
            meterData.setTelemetryValue("instantPower", newMeterPower * 10);
        }
        
        if (pcsData != null && powerBalance > 10) {
            double pcsPower = getDoubleValue(pcsData.getTelemetryValue("activePower")) / 10.0;
            double newPcsPower = Math.max(-100, pcsPower - powerBalance * 0.05);
            pcsData.setTelemetryValue("activePower", newPcsPower * 10);
        }
    }

    private void adjustDcBusPower(Topology topology, Bus bus, double powerBalance) {
        DeviceRuntimeData pcsData = null;
        DeviceRuntimeData mpptData = null;
        
        for (String deviceId : bus.getConnectedDevices()) {
            Device device = topology.getDevice(deviceId);
            if (device == null) continue;
            
            if ("PCS".equals(device.getType())) {
                pcsData = runtimeStateService.getRuntimeData(deviceId);
            } else if ("MPPT".equals(device.getType())) {
                mpptData = runtimeStateService.getRuntimeData(deviceId);
            }
        }
        
        if (pcsData != null && powerBalance < -10) {
            double dcPower = getDoubleValue(pcsData.getTelemetryValue("dcPower")) / 10.0;
            double newDcPower = Math.min(100, dcPower - powerBalance * 0.1);
            pcsData.setTelemetryValue("dcPower", newDcPower * 10);
        }
    }

    private Double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    public Map<String, Double> getBusPowerMap() {
        return new HashMap<>(busPowerMap);
    }

    public double getBusPower(String busId) {
        return busPowerMap.getOrDefault(busId, 0.0);
    }

    public double getSimulationTime() {
        return simulationTime;
    }
}