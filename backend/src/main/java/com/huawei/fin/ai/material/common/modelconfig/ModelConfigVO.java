package com.huawei.fin.ai.material.common.modelconfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ModelConfigVO {

    private Long id;
    private String name;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String capabilities;
    private BigDecimal temperature;
    private Integer timeoutSeconds;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
