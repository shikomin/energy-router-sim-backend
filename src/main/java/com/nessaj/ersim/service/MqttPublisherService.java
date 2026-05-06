package com.nessaj.ersim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nessaj.ersim.config.MqttProperties;
import com.nessaj.ersim.model.DeviceRuntimeData;
import com.nessaj.ersim.model.Point;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@Service
public class MqttPublisherService {

    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;

    private MqttClient mqttClient;
    private final Map<String, Integer> qosMap = new ConcurrentHashMap<>();
    private final Map<String, String> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000;
    private static final long MAX_RECONNECT_DELAY_MS = 30000;

    private Consumer<String> messageCallback;
    private final ExecutorService publishExecutor;
    private final RuntimeStateService runtimeStateService;

    public MqttPublisherService(MqttProperties mqttProperties, ObjectMapper objectMapper,
                                @Autowired(required = false) RuntimeStateService runtimeStateService) {
        this.mqttProperties = mqttProperties;
        this.objectMapper = objectMapper;
        this.runtimeStateService = runtimeStateService;
        this.publishExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "mqtt-publish");
            t.setDaemon(true);
            return t;
        });
    }

    @Data
    public static class PointData {
        private String ptId;
        private String signalType;
        private String deviceLocalNum;
        private String linkedProp;
        private Object value;
        private long timestamp;
        private String unit;

        public PointData() {
            this.timestamp = Instant.now().toEpochMilli();
        }
    }

    @Data
    public static class DeviceData {
        private String deviceId;
        private String deviceName;
        private String deviceType;
        private Map<String, Object> properties;
        private long timestamp;
    }

    @PostConstruct
    public void init() {
        connect();
    }

    @PreDestroy
    public void disconnect() {
        isConnected.set(false);
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT client disconnected gracefully");
            }
        } catch (MqttException e) {
            log.error("Error disconnecting MQTT client: {}", e.getMessage());
        }
    }

    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    private synchronized void connect() {
        if (isConnected.get() && mqttClient != null && mqttClient.isConnected()) {
            return;
        }

        try {
            String brokerUrl = "tcp://" + mqttProperties.getHost() + ":" + mqttProperties.getPort();
            String clientId = mqttProperties.getClientId() + "-" + UUID.randomUUID().toString().substring(0, 8);

            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setServerURIs(new String[]{brokerUrl});
            options.setUserName(mqttProperties.getUsername());
            options.setPassword(mqttProperties.getPassword().toCharArray());
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(30);
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    isConnected.set(false);
                    log.warn("MQTT connection lost: {}", cause.getMessage());
                    scheduleReconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.debug("Received message from topic [{}]: {}", topic, payload);

                    if (messageCallback != null) {
                        messageCallback.accept(payload);
                    }

                    processQueue();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            isConnected.set(true);
            reconnectAttempts.set(0);
            log.info("MQTT client connected successfully to {} with client ID {}", brokerUrl, clientId);

            subscribe(mqttProperties.getSubscribeTopic());
            reconnectPendingMessages();
            processQueue();

        } catch (MqttException e) {
            isConnected.set(false);
            log.error("Failed to connect MQTT client: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void subscribe(String topic) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("Cannot subscribe to {}: not connected", topic);
            return;
        }

        try {
            mqttClient.subscribe(topic, 1);
            log.info("Subscribed to topic: {}", topic);
        } catch (MqttException e) {
            log.error("Failed to subscribe to topic {}: {}", topic, e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPTS) {
            log.error("Max reconnect attempts reached, giving up");
            return;
        }

        int attempt = reconnectAttempts.incrementAndGet();
        long delay = Math.min(INITIAL_RECONNECT_DELAY_MS * (long) Math.pow(2, attempt - 1), MAX_RECONNECT_DELAY_MS);

        log.info("Scheduling MQTT reconnect attempt {} in {} ms", attempt, delay);

        new Thread(() -> {
            try {
                Thread.sleep(delay);
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Reconnect thread interrupted");
            }
        }).start();
    }

    private void reconnectPendingMessages() {
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Reconnecting {} pending messages", pendingMessages.size());
        for (Map.Entry<String, String> entry : pendingMessages.entrySet()) {
            try {
                publishInternal(entry.getKey(), entry.getValue());
                pendingMessages.remove(entry.getKey());
            } catch (Exception e) {
                log.error("Failed to resend pending message to topic {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private void processQueue() {
        String message;
        while ((message = messageQueue.poll()) != null) {
            try {
                publishToTopic(mqttProperties.getPublishTopic(), message);
            } catch (Exception e) {
                log.error("Failed to process queued message: {}", e.getMessage());
            }
        }
    }

    public void publishPointData(String ptId, String signalType, String deviceLocalNum,
                                 String linkedProp, Object value, String unit) {
        PointData pointData = new PointData();
        pointData.setPtId(ptId);
        pointData.setSignalType(signalType);
        pointData.setDeviceLocalNum(deviceLocalNum);
        pointData.setLinkedProp(linkedProp);
        pointData.setValue(value);
        pointData.setUnit(unit);

        try {
            String payload = objectMapper.writeValueAsString(pointData);
            publishToTopic(mqttProperties.getPublishTopic(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize point data: {}", e.getMessage());
        }
    }

    public void publishManualPointData(String deviceId, String ptId, String signalType,
                                       String linkedProp, String value, String unit) {
        long timestamp = Instant.now().toEpochMilli();
        Map<String, Object> message = buildBaseMessage(timestamp, signalType);

        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("device_id", deviceId);

        Map<String, Object> dataObj = new HashMap<>();
        dataObj.put("data_id", ptId);
        dataObj.put("real_value", value);

        deviceData.put("data_objects", List.of(dataObj));
        Map<String, Object> stationData = (Map<String, Object>) message.get("station_data");
        stationData.put("device_data", List.of(deviceData));

        try {
            String payload = objectMapper.writeValueAsString(message);
            publishToTopic(mqttProperties.getPublishTopic(), payload);
            log.info("Manual point data sent - deviceId: {}, ptId: {}, type: {}, value: {}",
                    deviceId, ptId, signalType, value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize manual point data: {}", e.getMessage());
        }
    }

    public void publishDeviceData(String deviceId, String deviceName, String deviceType,
                                  Map<String, Object> properties) {
        DeviceData deviceData = new DeviceData();
        deviceData.setDeviceId(deviceId);
        deviceData.setDeviceName(deviceName);
        deviceData.setDeviceType(deviceType);
        deviceData.setProperties(properties);
        deviceData.setTimestamp(Instant.now().toEpochMilli());

        try {
            String payload = objectMapper.writeValueAsString(deviceData);
            publishToTopic(mqttProperties.getPublishTopic(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize device data: {}", e.getMessage());
        }
    }

    public void publishToDefaultTopic(Object data) {
        try {
            String payload = objectMapper.writeValueAsString(data);
            publishToTopic(mqttProperties.getPublishTopic(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data: {}", e.getMessage());
        }
    }

    public void publishToTopic(String topic, String payload) {
        if (!isConnected.get()) {
            log.warn("MQTT not connected, queueing message for topic: {}", topic);
            messageQueue.offer(payload);
            connect();
            return;
        }
        try {
            publishInternal(topic, payload);
            log.info("发布点位数据:{}", payload);
        } catch (MqttException e) {
            log.error("Failed to publish to topic {}, queueing for retry: {}", topic, e.getMessage());
            messageQueue.offer(payload);
            isConnected.set(false);
            scheduleReconnect();
        }
    }

    private void publishInternal(String topic, String payload) throws MqttException {
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }

        int qos = qosMap.getOrDefault(topic, 1);
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(qos);
        message.setRetained(false);

        mqttClient.publish(topic, message);
        log.debug("Published to topic [{}]: {}", topic, payload);
    }

    @Scheduled(fixedDelay = 30000)
    public void healthCheck() {
        if (!isConnected.get()) {
            log.warn("MQTT health check: not connected, attempting reconnect");
            connect();
        } else {
            try {
                if (mqttClient != null && mqttClient.isConnected()) {
                    log.debug("MQTT health check: connection OK");
                }
            } catch (Exception e) {
                log.error("MQTT health check failed: {}", e.getMessage());
                isConnected.set(false);
                connect();
            }
        }
    }

    public boolean isConnected() {
        return isConnected.get() && mqttClient != null && mqttClient.isConnected();
    }

    public Map<String, Object> buildPointMessage(String ptId, String signalType, String deviceLocalNum,
                                                 String linkedProp, Object value, String unit) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("ptId", ptId);
        msg.put("signalType", signalType);
        msg.put("deviceLocalNum", deviceLocalNum);
        msg.put("linkedProp", linkedProp);
        msg.put("value", value);
        msg.put("unit", unit);
        msg.put("timestamp", Instant.now().toEpochMilli());
        return msg;
    }

    public void publishSimulationData(Map<String, DeviceRuntimeData> runtimeDataMap, List<Point> allPoints) {
        long startTime = System.currentTimeMillis();
        long timestamp = Instant.now().toEpochMilli();

        List<Point> ycPoints = new ArrayList<>();
        List<Point> yxPoints = new ArrayList<>();

        for (Point point : allPoints) {
            if (point.getLinkedProp() == null || point.getLinkedProp().isEmpty()) {
                continue;
            }

            String deviceId = point.getDeviceLocalNum();
            if (runtimeStateService != null) {
                String mappedId = runtimeStateService.getDeviceIdByLocalNum(point.getDeviceLocalNum());
                if (mappedId != null) {
                    deviceId = mappedId;
                }
            }

            DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
            if (runtimeData == null) {
                continue;
            }

            Object value = null;
            if ("yc".equals(point.getSignalType())) {
                value = runtimeData.getTelemetryValue(point.getLinkedProp());
            } else if ("yx".equals(point.getSignalType())) {
                value = runtimeData.getTelesignalValue(point.getLinkedProp());
            }

            if (value == null) {
                continue;
            }

            if ("yc".equals(point.getSignalType())) {
                ycPoints.add(point);
            } else if ("yx".equals(point.getSignalType())) {
                yxPoints.add(point);
            }
        }

        Map<String, List<Point>> ycByDevice = groupPointsByDevice(ycPoints);
        Map<String, List<Point>> yxByDevice = groupPointsByDevice(yxPoints);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        if (!ycByDevice.isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                publishYcData(timestamp, ycByDevice, runtimeDataMap);
            }, publishExecutor));
        }

        if (!yxByDevice.isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                publishYxData(timestamp, yxByDevice, runtimeDataMap);
            }, publishExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 100) {
            log.warn("MQTT publish took {}ms, points: yc={}, yx={}", elapsed, ycPoints.size(), yxPoints.size());
        } else {
            log.debug("MQTT publish completed in {}ms: yc={}, yx={}", elapsed, ycPoints.size(), yxPoints.size());
        }
    }

    private Map<String, List<Point>> groupPointsByDevice(List<Point> points) {
        Map<String, List<Point>> grouped = new ConcurrentHashMap<>();
        for (Point point : points) {
            grouped.computeIfAbsent(point.getDeviceLocalNum(), k -> new ArrayList<>()).add(point);
        }
        return grouped;
    }

    private void publishYcData(long timestamp, Map<String, List<Point>> pointsByDevice,
                               Map<String, DeviceRuntimeData> runtimeDataMap) {
        Map<String, Object> ycMessage = buildBaseMessage(timestamp, "yc");

        List<Map<String, Object>> deviceDataList = new ArrayList<>();
        for (Map.Entry<String, List<Point>> entry : pointsByDevice.entrySet()) {
            String deviceId = entry.getKey();
            List<Point> points = entry.getValue();

            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("device_id", deviceId);

            List<Map<String, Object>> dataObjects = new ArrayList<>();
            for (Point point : points) {
                DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
                if (runtimeData == null) continue;

                Object value = runtimeData.getTelemetryValue(point.getLinkedProp());
                if (value == null) continue;

                Map<String, Object> dataObj = new HashMap<>();
                dataObj.put("data_id", point.getPtId());
                dataObj.put("real_value", String.valueOf(value));
                dataObjects.add(dataObj);
            }

            if (!dataObjects.isEmpty()) {
                deviceData.put("data_objects", dataObjects);
                deviceDataList.add(deviceData);
            }
        }

        if (!deviceDataList.isEmpty()) {
            ycMessage.put("device_data", deviceDataList);
            try {
                String payload = objectMapper.writeValueAsString(ycMessage);
                publishToTopic(mqttProperties.getPublishTopic(), payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize YC message: {}", e.getMessage());
            }
        }
    }

    private void publishYxData(long timestamp, Map<String, List<Point>> pointsByDevice,
                               Map<String, DeviceRuntimeData> runtimeDataMap) {
        Map<String, Object> yxMessage = buildBaseMessage(timestamp, "yx");

        List<Map<String, Object>> deviceDataList = new ArrayList<>();
        for (Map.Entry<String, List<Point>> entry : pointsByDevice.entrySet()) {
            String deviceId = entry.getKey();
            List<Point> points = entry.getValue();

            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("device_id", deviceId);

            List<Map<String, Object>> dataObjects = new ArrayList<>();
            for (Point point : points) {
                DeviceRuntimeData runtimeData = runtimeDataMap.get(deviceId);
                if (runtimeData == null) continue;

                Object value = runtimeData.getTelesignalValue(point.getLinkedProp());
                if (value == null) continue;

                Map<String, Object> dataObj = new HashMap<>();
                dataObj.put("data_id", point.getPtId());
                dataObj.put("real_value", String.valueOf(value));
                dataObjects.add(dataObj);
            }

            if (!dataObjects.isEmpty()) {
                deviceData.put("data_objects", dataObjects);
                deviceDataList.add(deviceData);
            }
        }

        if (!deviceDataList.isEmpty()) {
            yxMessage.put("device_data", deviceDataList);
            try {
                String payload = objectMapper.writeValueAsString(yxMessage);
                publishToTopic(mqttProperties.getPublishTopic(), payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize YX message: {}", e.getMessage());
            }
        }
    }

    private Map<String, Object> buildBaseMessage(long timestamp, String type) {
        Map<String, Object> message = new HashMap<>();
        message.put("station_id", mqttProperties.getStationId());
        message.put("datatype", 2);
        message.put("timestamp", timestamp / 1000);

        Map<String, Object> stationData = new HashMap<>();
        stationData.put("type", type);
        message.put("station_data", stationData);

        return message;
    }
}