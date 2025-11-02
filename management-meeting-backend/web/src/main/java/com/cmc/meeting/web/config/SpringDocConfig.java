package com.cmc.meeting.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Meeting Room Booking API")
                        .version("1.0.0")
                        .description("Hệ thống API quản lý và đặt lịch họp")
                        .license(new License().name("CMC Global").url("https://cmcglobal.com.vn/")));
    }
}