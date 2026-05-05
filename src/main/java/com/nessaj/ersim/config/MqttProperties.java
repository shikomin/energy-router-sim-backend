package com.nessaj.ersim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String clientId;
    private String publishTopic;
    private String subscribeTopic;
    private long stationId;
}