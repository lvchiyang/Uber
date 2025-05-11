package com.lvchiyang.Driver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 应用配置类
 * 提供全局的Bean配置
 */
@Configuration
public class ApplicationConfig {

    /**
     * 配置RestTemplate，用于服务间通信
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}