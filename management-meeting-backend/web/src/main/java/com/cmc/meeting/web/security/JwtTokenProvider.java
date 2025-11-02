package com.cmc.meeting.web.security;

import com.cmc.meeting.application.port.security.TokenProvider;
import com.cmc.meeting.web.config.JwtConfigProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider{

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfigProperties jwtConfig;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(JwtConfigProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
        
        // 1. Giải mã Secret Key từ Base64 (lấy từ .env)
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        
        // 2. Tạo một parser (bộ giải mã) dùng chung
        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .build();
    }

    /**
     * Tạo một JWT mới cho user.
     * Subject (chủ thể) BÂY GIỜ LÀ USERNAME.
     */
    @Override
    public String generateToken(Long userId, String username) { // Vẫn nhận vào cả 2
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        // Tạo token
        return Jwts.builder()
                .subject(username) // <-- THAY ĐỔI QUAN TRỌNG: Dùng username làm subject
                .claim("userId", userId) // <-- Thêm userId như một claim
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.secretKey)
                .compact();
    }
    /**
     * Giải mã token và lấy ra USERNAME (từ subject)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token)
                                 .getPayload();
        
        return claims.getSubject(); // Lấy Subject (chính là username)
    }
    /**
     * Giải mã token và lấy ra ID của user
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token)
                                 .getPayload();
        
        return claims.get("userId", Long.class); // Lấy "userId" từ claim
    }

    /**
     * Kiểm tra xem token có hợp lệ hay không
     */
    public boolean validateToken(String token) {
        try {
            // Chỉ cần parse token. Nếu parse thành công = token hợp lệ.
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }
}