package com.nessaj.ersim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Auther 胥珂铭
 * 创建时间: 2026-04-30
 * 功能描述:
 **/
@Slf4j
@ComponentScan("com.nessaj")
@SpringBootApplication
@EnableScheduling
public class EnergyRouterSimulatorApp {

    public static void main(String[] args) {
        log.info("#=====>> EnergyRouterSimulatorApp服务启动 <<=====#");
        SpringApplication.run(EnergyRouterSimulatorApp.class);
    }
}