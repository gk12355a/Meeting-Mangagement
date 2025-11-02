package com.cmc.meeting.web.security;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // Inject Port của Domain

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User domainUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));
        
        // BỔ SUNG: Kiểm tra xem tài khoản có bị vô hiệu hóa không
        if (!domainUser.isActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa.");
        }
        
        Set<GrantedAuthority> authorities = domainUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                domainUser.getUsername(),
                domainUser.getPassword(),
                authorities
        );
    }
}