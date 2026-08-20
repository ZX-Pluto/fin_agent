package com.huawei.fin.ai.material.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String baseUrl = "http://127.0.0.1:8088/v1";
    private String apiKey = "sk-placeholder";
    private String model = "qwen-v3-32b";
    private double temperature = 0.3;
    private int timeoutSeconds = 60;
}
