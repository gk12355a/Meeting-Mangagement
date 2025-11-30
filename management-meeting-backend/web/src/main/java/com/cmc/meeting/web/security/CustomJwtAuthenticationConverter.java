package com.cmc.meeting.web.security;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public CustomJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // [SỬA LỖI FINAL]: Sử dụng một biến tạm thời để xử lý giá trị
        Long authId; 
        Object authIdClaim = jwt.getClaim("auth_id");
        
        if (authIdClaim instanceof Number) {
            // Gán giá trị (biến authId trở thành effectively final sau khối này)
            authId = ((Number) authIdClaim).longValue();
        } else {
            // Đảm bảo biến được gán giá trị trong mọi trường hợp (bao gồm cả null)
            authId = null; 
        }
        
        // Kiểm tra tính hợp lệ
        if (authId == null) {
            throw new EntityNotFoundException("JWT thiếu claim 'auth_id' cần thiết.");
        }

        // 2. Tìm kiếm User trong DB Backend bằng Auth ID
        User user = userRepository.findByAuthServiceId(authId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy User trong Meeting DB với Auth ID: " + authId));
        
        // 3. Xây dựng Authorities (Roles) từ Domain Model
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toSet());

        // 4. Trả về đối tượng xác thực Resource Server
        return new JwtAuthenticationToken(jwt, authorities, user.getUsername());
    }
}