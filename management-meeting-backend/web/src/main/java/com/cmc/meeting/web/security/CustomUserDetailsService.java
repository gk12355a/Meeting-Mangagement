package com.cmc.meeting.web.security;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection; // THÊM IMPORT NÀY
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User domainUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));
        
        if (!domainUser.isActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa.");
        }
        
        Set<GrantedAuthority> authorities = domainUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());

        // THAY ĐỔI QUAN TRỌNG:
        // Thay vì trả về lớp User chuẩn, trả về lớp CustomUserDetails
        // và truyền 'domainUser.getId()' vào.
        return new CustomUserDetails(
                domainUser.getId(), 
                domainUser.getUsername(),
                domainUser.getPassword(),
                authorities
        );
    }

    // =================================================================
    // === BỔ SUNG LỚP NÀY VÀO (LỚP MÀ CHATBOTCONTROLLER CẦN) ===
    // =================================================================
    public static class CustomUserDetails implements UserDetails {

        private final Long id;
        private final String username;
        private final String password;
        private final Set<GrantedAuthority> authorities;

        public CustomUserDetails(Long id, String username, String password, Set<GrantedAuthority> authorities) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.authorities = authorities;
        }

        // Đây là hàm mà ChatbotController cần
        public Long getId() {
            return id;
        }

        // --- Các hàm bắt buộc của interface UserDetails ---
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true; // Đã check 'isActive' ở hàm loadUserByUsername
        }
    }
}