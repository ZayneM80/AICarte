package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.zhipu")
@Data
public class ZhiPuProperties {

    private String apiKey;
    private String baseUrl;
    private String model;
    private String chatModel;
    private String chatBaseUrl;
    private String chatApiKey;

}
