package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.auth.AuthResponse;
import com.cmc.meeting.application.dto.auth.LoginRequest;
import com.cmc.meeting.application.dto.auth.RegisterRequest;
import com.cmc.meeting.application.port.service.AuthService;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.UserRepository;
import com.cmc.meeting.application.port.security.TokenProvider;
import com.cmc.meeting.domain.model.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider; // <-- 2. Sửa kiểu dữ liệu

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TokenProvider tokenProvider) { // <-- 3. Sửa kiểu dữ liệu
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // 1. Xác thực username/password (dùng Spring Security)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. Nếu xác thực thành công, set vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Lấy thông tin user (để tạo token)
        // Cần lấy ID và Username
        User user = userRepository.findByUsername(loginRequest.getUsername()).get();

        // 4. Tạo token
        String token = tokenProvider.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(token);
    }

    @Override
    public String register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Lỗi: Username (Email) đã được sử dụng!");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setFullName(registerRequest.getFullName());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        // BỔ SUNG: Gán quyền mặc định
        newUser.getRoles().add(Role.ROLE_USER);
        
        userRepository.save(newUser);
        return "Đăng ký người dùng thành công!";
    }
}