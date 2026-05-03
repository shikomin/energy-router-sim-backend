package com.nessaj.ersim.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 订阅控制器
 * 处理客户端的订阅请求和订阅管理
 */
@Slf4j
@Controller
@RequestMapping("/ws")
public class WebSocketController {

    /**
     * 订阅者管理（用于统计）
     */
    private final Map<String, String> subscribers = new ConcurrentHashMap<>();

    /**
     * 订阅设备状态更新
     */
    @MessageMapping("/subscribe/devices")
    @SendTo("/topic/devices")
    public Map<String, Object> subscribeDevices(String deviceId) {
        log.info("Client subscribed to device updates: {}", deviceId);
        return Map.of(
                "status", "success",
                "topic", "/topic/devices",
                "message", "Successfully subscribed to device updates"
        );
    }

    /**
     * 订阅模拟状态更新
     */
    @MessageMapping("/subscribe/simulation")
    @SendTo("/topic/simulation")
    public Map<String, Object> subscribeSimulation() {
        log.info("Client subscribed to simulation updates");
        return Map.of(
                "status", "success",
                "topic", "/topic/simulation",
                "message", "Successfully subscribed to simulation updates"
        );
    }

    /**
     * 订阅拓扑状态更新
     */
    @MessageMapping("/subscribe/topology")
    @SendTo("/topic/topology")
    public Map<String, Object> subscribeTopology() {
        log.info("Client subscribed to topology updates");
        return Map.of(
                "status", "success",
                "topic", "/topic/topology",
                "message", "Successfully subscribed to topology updates"
        );
    }

    /**
     * 获取可用订阅主题列表
     */
    @GetMapping("/topics")
    @ResponseBody
    public Map<String, Object> getTopics() {
        return Map.of(
                "topics", Map.of(
                        "devices", "/topic/devices",
                        "deviceById", "/topic/devices/{deviceId}",
                        "simulation", "/topic/simulation",
                        "topology", "/topic/topology",
                        "system", "/topic/system"
                ),
                "description", "WebSocket 订阅主题列表"
        );
    }

    /**
     * 获取当前订阅者数量
     */
    @GetMapping("/subscribers")
    @ResponseBody
    public Map<String, Object> getSubscriberCount() {
        return Map.of(
                "count", subscribers.size(),
                "description", "当前 WebSocket 订阅者数量"
        );
    }
}