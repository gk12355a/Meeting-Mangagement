package com.cmc.meeting.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt") // Lấy các giá trị có tiền tố "app.jwt"
public class JwtConfigProperties {
    private String secret;         // Sẽ được gán bằng ${JWT_SECRET}
    private long expirationMs; // Sẽ được gán bằng ${JWT_EXPIRATION_MS}
}