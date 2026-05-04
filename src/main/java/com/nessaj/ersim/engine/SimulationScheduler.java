package com.nessaj.ersim.engine;

import com.nessaj.ersim.service.SimulationControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SimulationScheduler {

    private final SimulationControlService simulationControlService;
    private final PowerSimulationEngine simulationEngine;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile int stepIntervalMs = 1000;

    public SimulationScheduler(@Lazy SimulationControlService simulationControlService,
                              PowerSimulationEngine simulationEngine) {
        this.simulationControlService = simulationControlService;
        this.simulationEngine = simulationEngine;
    }

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimulationScheduler");
            t.setDaemon(true);
            return t;
        });
        log.info("SimulationScheduler daemon thread initialized");
    }

    @PreDestroy
    public void shutdown() {
        active.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("SimulationScheduler shutdown");
    }

    public void activate() {
        if (active.compareAndSet(false, true)) {
            scheduleNextStep();
            log.info("SimulationScheduler activated");
        }
    }

    public void deactivate() {
        if (active.compareAndSet(true, false)) {
            log.info("SimulationScheduler deactivated");
        }
    }

    private void scheduleNextStep() {
        if (!active.get()) {
            return;
        }
        scheduler.schedule(() -> {
            try {
                if (active.get() && runtimeStateServiceRunning()) {
                    int interval = simulationControlService.getStepIntervalMs();
                    simulationEngine.simulateStep(1.0);
                    simulationControlService.addSimulationTime(interval);
                }
            } catch (Exception e) {
                log.error("Error in simulation step", e);
            } finally {
                if (active.get()) {
                    scheduleNextStep();
                }
            }
        }, stepIntervalMs, TimeUnit.MILLISECONDS);
    }

    private boolean runtimeStateServiceRunning() {
        try {
            return simulationControlService.isSimulationRunning();
        } catch (Exception e) {
            return false;
        }
    }

    public void setStepIntervalMs(int intervalMs) {
        if (intervalMs > 0 && intervalMs != this.stepIntervalMs) {
            this.stepIntervalMs = intervalMs;
            log.info("SimulationScheduler stepInterval changed to {}ms", intervalMs);
        }
    }

    public int getStepIntervalMs() {
        return stepIntervalMs;
    }

    public boolean isRunning() {
        return active.get();
    }
}