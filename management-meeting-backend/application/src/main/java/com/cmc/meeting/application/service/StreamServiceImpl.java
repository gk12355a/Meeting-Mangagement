package com.cmc.meeting.application.service;

import com.cmc.meeting.application.port.service.StreamService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamServiceImpl implements StreamService {

    @Value("${stream.api-key}")
    private String apiKey;

    @Value("${stream.secret-key}")
    private String secretKey;

    @Value("${stream.token-expiration:3600}")
    private long expirationTime; // Mặc định 1 giờ nếu không cấu hình

    @Override
    public String createToken(String userId) {
        log.info("Generating Stream Video Token for UserID: {}", userId);

        // Stream yêu cầu user_id phải nằm trong claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        
        // Có thể thêm custom claims nếu cần (role, permission...)
        // claims.put("role", "admin"); 

        long now = System.currentTimeMillis();
        long exp = now + (expirationTime * 1000);

        try {
            // Ký token bằng thuật toán HS256 và Secret Key của Stream
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId)
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(exp))
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error creating Stream Token", e);
            throw new RuntimeException("Could not generate video call token", e);
        }
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}