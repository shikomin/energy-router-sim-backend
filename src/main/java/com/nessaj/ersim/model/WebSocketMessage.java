package com.nessaj.ersim.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息模型
 * 用于后端向前端推送实时数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 主题名称
     */
    private String topic;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 消息时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 设备状态更新
         */
        DEVICE_UPDATE,
        
        /**
         * 模拟状态更新
         */
        SIMULATION_UPDATE,
        
        /**
         * 拓扑状态更新
         */
        TOPOLOGY_UPDATE,
        
        /**
         * 系统事件
         */
        SYSTEM_EVENT,
        
        /**
         * 心跳消息
         */
        HEARTBEAT
    }

    /**
     * 创建设备更新消息
     */
    public static <T> WebSocketMessage<T> deviceUpdate(String topic, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.DEVICE_UPDATE)
                .topic(topic)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建模拟状态更新消息
     */
    public static <T> WebSocketMessage<T> simulationUpdate(String topic, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.SIMULATION_UPDATE)
                .topic(topic)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建拓扑更新消息
     */
    public static <T> WebSocketMessage<T> topologyUpdate(String topic, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.TOPOLOGY_UPDATE)
                .topic(topic)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建系统事件消息
     */
    public static <T> WebSocketMessage<T> systemEvent(String topic, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.SYSTEM_EVENT)
                .topic(topic)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}