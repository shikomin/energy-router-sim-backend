package com.nessaj.ersim.websvr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther 胥珂铭
 * 创建时间: 2026-04-30
 * 功能描述:
 **/
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping("/check")
    public String healthcheck(){
        return "the energy-router-simulator web server health check is ok.";
    }

}
