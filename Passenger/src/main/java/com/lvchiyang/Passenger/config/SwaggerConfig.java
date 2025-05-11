package com.lvchiyang.Passenger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Passenger Service API")
                        .description("乘客服务API文档")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("lvchiyang")
                                .email("lvchiyang@example.com")));
    }
}