package com.nessaj.ersim.service;

import com.nessaj.ersim.model.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 消息发布服务
 * 用于向客户端推送实时数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPublisherService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket 主题常量
     */
    public static final String TOPIC_DEVICE_UPDATES = "/topic/devices";
    public static final String TOPIC_DEVICE_PREFIX = "/topic/devices/";
    public static final String TOPIC_SIMULATION = "/topic/simulation";
    public static final String TOPIC_TOPOLOGY = "/topic/topology";
    public static final String TOPIC_SYSTEM = "/topic/system";
    public static final String TOPIC_MQTT_STATE = "/topic/mqtt_state";

    /**
     * 发布设备状态更新（广播给所有订阅者）
     */
    public void publishDeviceUpdate(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.deviceUpdate(TOPIC_DEVICE_UPDATES, data);
        messagingTemplate.convertAndSend(TOPIC_DEVICE_UPDATES, message);
        log.debug("Published device update to topic: {}", TOPIC_DEVICE_UPDATES);
    }

    /**
     * 发布特定设备状态更新
     */
    public void publishDeviceUpdate(String deviceId, Object data) {
        String topic = TOPIC_DEVICE_PREFIX + deviceId;
        WebSocketMessage<Object> message = WebSocketMessage.deviceUpdate(topic, data);
        messagingTemplate.convertAndSend(topic, message);
        log.debug("Published device update to topic: {}", topic);
    }

    /**
     * 发布模拟状态更新
     */
    public void publishSimulationUpdate(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.simulationUpdate(TOPIC_SIMULATION, data);
        messagingTemplate.convertAndSend(TOPIC_SIMULATION, message);
        log.debug("Published simulation update to topic: {}", TOPIC_SIMULATION);
    }

    /**
     * 发布拓扑状态更新
     */
    public void publishTopologyUpdate(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.topologyUpdate(TOPIC_TOPOLOGY, data);
        messagingTemplate.convertAndSend(TOPIC_TOPOLOGY, message);
        log.debug("Published topology update to topic: {}", TOPIC_TOPOLOGY);
    }

    /**
     * 发布系统事件
     */
    public void publishSystemEvent(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.systemEvent(TOPIC_SYSTEM, data);
        messagingTemplate.convertAndSend(TOPIC_SYSTEM, message);
        log.debug("Published system event to topic: {}", TOPIC_SYSTEM);
    }

    /**
     * 发布MQTT状态更新
     */
    public void publishMqttState(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.systemEvent(TOPIC_MQTT_STATE, data);
        messagingTemplate.convertAndSend(TOPIC_MQTT_STATE, message);
        log.debug("Published MQTT state to topic: {}", TOPIC_MQTT_STATE);
    }

    /**
     * 发布消息到指定主题
     */
    public void publishToTopic(String topic, Object data) {
        messagingTemplate.convertAndSend(topic, data);
        log.debug("Published message to topic: {}", topic);
    }
}