package com.nessaj.ersim.service;

import com.nessaj.ersim.config.ConfigurationLoader;
import com.nessaj.ersim.engine.PowerSimulationEngine;
import com.nessaj.ersim.model.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulationControlService {

    private final ConfigurationLoader configLoader;
    private final RuntimeStateService runtimeStateService;
    private final PowerSimulationEngine simulationEngine;

    private final AtomicBoolean running;
    private final AtomicLong simulationTime;
    private volatile SimulatorConfig simulatorConfig;
    private volatile boolean initialized;

    public SimulationControlService(ConfigurationLoader configLoader,
                                    RuntimeStateService runtimeStateService,
                                    PowerSimulationEngine simulationEngine) {
        this.configLoader = configLoader;
        this.runtimeStateService = runtimeStateService;
        this.simulationEngine = simulationEngine;
        this.running = new AtomicBoolean(false);
        this.simulationTime = new AtomicLong(0);
        this.initialized = false;
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
        running.set(true);
        simulationTime.set(0);
    }

    public void stopSimulation() {
        running.set(false);
        runtimeStateService.setSimulationRunning(false);
    }

    public void resetSimulation() {
        stopSimulation();
        runtimeStateService.clearAllRuntimeData();
        runtimeStateService.initializeRuntimeData();
        simulationTime.set(0);
    }

    @Scheduled(fixedRateString = "#{@simulationControlService.getStepIntervalMs()}")
    public void simulationStep() {
        if (!running.get()) {
            return;
        }

        double deltaTime = simulatorConfig.getTimeScale();
        simulationEngine.simulateStep(deltaTime);
        simulationTime.addAndGet((long) (simulatorConfig.getStepIntervalMs() * deltaTime));
    }

    public void stepSimulation() {
        if (!running.get()) {
            runtimeStateService.initializeRuntimeData();
            running.set(true);
            runtimeStateService.setSimulationRunning(true);
        }

        double deltaTime = simulatorConfig.getTimeScale() != null ? simulatorConfig.getTimeScale() : 1.0;
        simulationEngine.simulateStep(deltaTime);
        simulationTime.addAndGet((long) (simulatorConfig.getStepIntervalMs() * deltaTime));
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getSimulationTime() {
        return simulationTime.get();
    }

    public SimulatorConfig getSimulatorConfig() {
        return simulatorConfig;
    }

    public void updateSimulatorConfig(SimulatorConfig config) {
        this.simulatorConfig = config;
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
}