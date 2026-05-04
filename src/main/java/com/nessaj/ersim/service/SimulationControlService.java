package com.nessaj.ersim.service;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.engine.PowerSimulationEngine;
import com.nessaj.ersim.engine.SimulationScheduler;
import com.nessaj.ersim.model.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulationControlService {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;
    private final PowerSimulationEngine simulationEngine;
    private final SimulationScheduler simulationScheduler;

    private final AtomicLong simulationTime = new AtomicLong(0);
    private volatile SimulatorConfig simulatorConfig;
    private volatile boolean initialized;

    public SimulationControlService(ConfigurationLoader configLoader,
                                  RuntimeStateService runtimeStateService,
                                  PowerSimulationEngine simulationEngine,
                                  SimulationScheduler simulationScheduler) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
        this.simulationEngine = simulationEngine;
        this.simulationScheduler = simulationScheduler;
    }

    @PostConstruct
    public void init() {
        loadSimulatorConfig();
        initialized = true;
    }

    public void loadSimulatorConfig() {
        SimulatorConfigWrapper configWrapper = configLoader.loadSimulatorConfig();
        if (configWrapper != null && configWrapper.getSimulator() != null) {
            this.simulatorConfig = configWrapper.getSimulator();
        } else {
            this.simulatorConfig = createDefaultConfig();
        }
        if (this.simulatorConfig.getStepIntervalMs() != null) {
            simulationScheduler.setStepIntervalMs(this.simulatorConfig.getStepIntervalMs());
        }
    }

    private SimulatorConfig createDefaultConfig() {
        SimulatorConfig config = new SimulatorConfig();
        config.setStepIntervalMs(1000);
        config.setTimeScale(1.0);
        config.setEnabled(false);
        return config;
    }

    public void startSimulation() {
        if (!initialized) {
            loadSimulatorConfig();
            initialized = true;
        }

        if (runtimeStateService.isSimulationRunning()) {
            return;
        }

        runtimeStateService.initializeRuntimeData();
        runtimeStateService.setSimulationRunning(true);
        simulationTime.set(0);
        simulationScheduler.activate();
    }

    public void stopSimulation() {
        simulationScheduler.deactivate();
        runtimeStateService.setSimulationRunning(false);
    }

    public void resetSimulation() {
        stopSimulation();
        runtimeStateService.clearAllRuntimeData();
        runtimeStateService.initializeRuntimeData();
        simulationTime.set(0);
    }

    public void stepSimulation() {
        if (!runtimeStateService.isSimulationRunning()) {
            runtimeStateService.initializeRuntimeData();
            runtimeStateService.setSimulationRunning(true);
        }

        double deltaTime = simulatorConfig.getTimeScale() != null ? simulatorConfig.getTimeScale() : 1.0;
        simulationEngine.simulateStep(deltaTime);
        simulationTime.addAndGet(simulatorConfig.getStepIntervalMs());
    }

    public void addSimulationTime(long deltaMs) {
        simulationTime.addAndGet(deltaMs);
    }

    public boolean isRunning() {
        return simulationScheduler.isRunning() && runtimeStateService.isSimulationRunning();
    }

    public boolean isSimulationRunning() {
        return runtimeStateService.isSimulationRunning();
    }

    public long getSimulationTime() {
        return simulationTime.get();
    }

    public SimulatorConfig getSimulatorConfig() {
        return simulatorConfig;
    }

    public void updateSimulatorConfig(SimulatorConfig config) {
        this.simulatorConfig = config;
        if (config.getStepIntervalMs() != null) {
            simulationScheduler.setStepIntervalMs(config.getStepIntervalMs());
        }
        saveSimulatorConfig();
    }

    public void saveSimulatorConfig() {
        SimulatorConfigWrapper wrapper = new SimulatorConfigWrapper();
        wrapper.setSimulator(simulatorConfig);

        PowerBalanceConfig pbConfig = new PowerBalanceConfig();
        pbConfig.setTolerance(0.01);
        pbConfig.setMaxIterations(100);
        wrapper.setPowerBalance(pbConfig);

        DefaultsConfig defaultsConfig = new DefaultsConfig();
        defaultsConfig.setAcVoltage(380.0);
        defaultsConfig.setAcFrequency(50.0);
        defaultsConfig.setDcVoltage(800.0);
        wrapper.setDefaults(defaultsConfig);

        configLoader.saveSimulatorConfig(wrapper);
    }

    public int getStepIntervalMs() {
        return simulatorConfig != null && simulatorConfig.getStepIntervalMs() != null
                ? simulatorConfig.getStepIntervalMs() : 1000;
    }

    public void setPbatManualMode(boolean manual, double setpoint) {
        simulationEngine.setPbatManualMode(manual, setpoint);
    }

    public boolean isPbatManualMode() {
        return simulationEngine.isPbatManualMode();
    }

    public double getPbatManualSetpoint() {
        return simulationEngine.getPbatManualSetpoint();
    }

    public void setSystemState(String state) {
        if ("OFF_GRID".equalsIgnoreCase(state)) {
            simulationEngine.setSystemState(com.nessaj.ersim.engine.SystemState.OFF_GRID);
        } else {
            simulationEngine.setSystemState(com.nessaj.ersim.engine.SystemState.GRID_CONNECTED);
        }
    }

    public String getSystemState() {
        return simulationEngine.getSystemState().name();
    }

    public String getCurrentSubState() {
        return simulationEngine.getCurrentSubState();
    }
}