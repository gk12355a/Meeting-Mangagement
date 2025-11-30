package com.cmc.meeting.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            // [LOGIC HYBRID QUAN TRỌNG]
            // Chỉ xử lý nếu có JWT và SecurityContext chưa có ai đăng nhập
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Thử validate bằng Secret Key (Local Token)
                // Nếu validate thành công => Đây là Local User
                // Nếu thất bại (ném Exception) => Có thể là SSO Token => Bỏ qua để Resource Server xử lý
                try {
                    if (tokenProvider.validateToken(jwt)) {
                        String username = tokenProvider.getUsernameFromToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("✅ [Legacy Filter] Xác thực thành công token Local cho user: {}", username);
                    }
                } catch (Exception e) {
                    // [QUAN TRỌNG] "Nuốt" lỗi này. 
                    // Đừng log ERROR, vì đây có thể là token SSO hợp lệ mà Filter này không hiểu.
                    log.trace("Token không phải Local Token (có thể là SSO), chuyển tiếp cho Resource Server.");
                }
            }
        } catch (Exception ex) {
            log.error("Lỗi không xác định trong JwtAuthenticationFilter", ex);
        }

        // Chuyển tiếp request cho các filter sau (bao gồm OAuth2 Resource Server)
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}