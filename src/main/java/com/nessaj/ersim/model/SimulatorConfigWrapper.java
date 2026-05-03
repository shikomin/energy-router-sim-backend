package com.nessaj.ersim.model;

public class SimulatorConfigWrapper {
    private SimulatorConfig simulator;
    private PowerBalanceConfig powerBalance;
    private DefaultsConfig defaults;

    public SimulatorConfigWrapper() {}

    public SimulatorConfig getSimulator() { return simulator; }
    public void setSimulator(SimulatorConfig simulator) { this.simulator = simulator; }
    public PowerBalanceConfig getPowerBalance() { return powerBalance; }
    public void setPowerBalance(PowerBalanceConfig powerBalance) { this.powerBalance = powerBalance; }
    public DefaultsConfig getDefaults() { return defaults; }
    public void setDefaults(DefaultsConfig defaults) { this.defaults = defaults; }
}