package com.cmc.meeting.web.security;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; // [QUAN TRỌNG] Dùng class này
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
        // 1. Lấy auth_id từ Token
        Long authId;
        Object authIdClaim = jwt.getClaim("auth_id");
        
        if (authIdClaim instanceof Number) {
            authId = ((Number) authIdClaim).longValue();
        } else {
            authId = null;
        }
        
        if (authId == null) {
            throw new EntityNotFoundException("JWT thiếu claim 'auth_id' cần thiết.");
        }

        // 2. Tìm kiếm User trong DB Backend (Dữ liệu mới nhất)
        User user = userRepository.findByAuthServiceId(authId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy User trong Meeting DB với Auth ID: " + authId));
        
        // 3. Kiểm tra trạng thái hoạt động (Logic chặn user bị khóa)
        if (!user.isActive()) {
            throw new DisabledException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        // 4. Xây dựng Authorities (Roles) từ DB Backend
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toSet());

        // 5. [SỬA LỖI 404 TẠI ĐÂY]
        // Sử dụng JwtAuthenticationToken và truyền user.getUsername() vào tham số thứ 3 (name).
        // Điều này đảm bảo khi Controller gọi principal.getName(), nó nhận được "username" đúng (vd: admin)
        // chứ không phải là User Object.
        return new JwtAuthenticationToken(jwt, authorities, user.getUsername());
    }
}