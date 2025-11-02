package com.cmc.meeting.web.security;

import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // Dùng để tạo danh sách quyền (roles)
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Dùng port của domain để tìm user
        User domainUser = userRepository.findByUsername(username)
                .orElseThrow(() -> 
                        new UsernameNotFoundException("Không tìm thấy user: " + username));
        
        // 2. Chuyển đổi (map) sang UserDetails (Spring Security)
        // SỬA DÒNG NÀY:
        return new org.springframework.security.core.userdetails.User(
                domainUser.getUsername(),
                domainUser.getPassword(), // <-- TRẢ VỀ MẬT KHẨU BĂM THẬT
                new ArrayList<>() // Danh sách quyền (hiện tại là rỗng)
        );
    }
}