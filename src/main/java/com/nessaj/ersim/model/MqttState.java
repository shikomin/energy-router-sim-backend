package com.nessaj.ersim.model;

import lombok.Data;

/**
 * MQTT连接状态信息
 */
@Data
public class MqttState {
    /**
     * 连接状态
     */
    private boolean connected;

    /**
     * MQTT Broker地址 (ip:port)
     */
    private String brokerAddress;

    /**
     * 本次模拟发送消息数
     */
    private long messageCount;

    /**
     * 本次模拟发送点位数量
     */
    private long pointCount;

    /**
     * 重置计数器
     */
    public void reset() {
        this.messageCount = 0;
        this.pointCount = 0;
    }

    public MqttState() {
        this.messageCount = 0;
        this.pointCount = 0;
        this.connected = false;
    }
}
