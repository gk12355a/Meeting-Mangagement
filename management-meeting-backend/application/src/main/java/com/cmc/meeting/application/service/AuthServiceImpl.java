package com.cmc.meeting.application.service;

// DTOs
import com.cmc.meeting.application.dto.auth.AuthResponse;
import com.cmc.meeting.application.dto.auth.ChangePasswordRequest;
import com.cmc.meeting.application.dto.auth.LoginRequest;
import com.cmc.meeting.application.dto.auth.RegisterRequest;
import com.cmc.meeting.application.dto.auth.ForgotPasswordRequest;
import com.cmc.meeting.application.dto.auth.ResetPasswordRequest;

// Ports (Hợp đồng)
import com.cmc.meeting.application.port.service.AuthService;
import com.cmc.meeting.application.port.notification.EmailNotificationPort;
import com.cmc.meeting.application.port.security.TokenProvider;

// Domain (Lõi nghiệp vụ)
import com.cmc.meeting.domain.model.PasswordResetToken;
import com.cmc.meeting.domain.model.Role;
import com.cmc.meeting.domain.model.User;
import com.cmc.meeting.domain.port.repository.PasswordResetTokenRepository;
import com.cmc.meeting.domain.port.repository.UserRepository;

// Spring & Java
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set; // Bổ sung
import java.util.stream.Collectors; // Bổ sung
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    // Dependencies (Inject qua Constructor)
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailNotificationPort emailSender;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailNotificationPort emailSender) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailSender = emailSender;
    }

    /**
     * API Đăng nhập
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // 1. Xác thực (như cũ)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        // 2. Set vào SecurityContext (như cũ)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Lấy thông tin user (như cũ)
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Lỗi logic: User đã xác thực nhưng không tìm thấy"));

        // 4. BỔ SUNG: Lấy Roles
        Set<String> roles = user.getRoles().stream()
                .map(Role::name) // Chuyển "ROLE_USER" thành String
                .collect(Collectors.toSet());

        // 5. Tạo token (Truyền 'roles' vào)
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), roles);

        return new AuthResponse(token);
    }

    /**
     * API Đăng ký
     */
    @Override
    public String register(RegisterRequest registerRequest) {
        // 1. Kiểm tra username (email) đã tồn tại chưa
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Lỗi: Username (Email) đã được sử dụng!");
        }

        // 2. Tạo đối tượng User mới
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setFullName(registerRequest.getFullName());

        // 3. Băm mật khẩu
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // 4. Gán quyền mặc định (ROLE_USER)
        newUser.getRoles().add(Role.ROLE_USER);

        // 5. Lưu vào CSDL
        userRepository.save(newUser);

        return "Đăng ký người dùng thành công!";
    }

    /**
     * (BS-5.1 & 5.2) Yêu cầu Đặt lại Mật khẩu & Gửi Mail
     */
    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getEmail());

        if (userOpt.isEmpty()) {
            // Không bao giờ báo "Không tìm thấy email" (vì lý do bảo mật)
            log.warn("Yêu cầu reset mật khẩu cho email không tồn tại: {}", request.getEmail());
            return "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu.";
        }

        User user = userOpt.get();

        // 2. Tạo Token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15); // Hết hạn sau 15p

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        // 3. Gửi Mail (Gọi Port)
        // (Adapter ở infrastructure sẽ lo việc tạo HTML và gửi)
        emailSender.sendPasswordResetEmail(user, token);

        return "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu.";
    }

    /**
     * (BS-5.3) Hoàn tất Đặt lại Mật khẩu
     */
    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        // 1. Tìm token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new EntityNotFoundException("Token không hợp lệ."));

        // 2. Kiểm tra hết hạn
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken); // Xóa token cũ
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu lại.");
        }

        // 3. Lấy user và cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. Xóa token (chỉ dùng 1 lần)
        passwordResetTokenRepository.delete(resetToken);

        return "Đặt lại mật khẩu thành công.";
    }

    @Override
    @Transactional
    public void changePassword(Long currentUserId, ChangePasswordRequest request) {
        // 1. Lấy user
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        // 2. KIỂM TRA MẬT KHẨU CŨ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            // Dùng lỗi này để GlobalExceptionHandler trả về 401
            throw new BadCredentialsException("Mật khẩu cũ không chính xác.");
        }

        // 3. (Tùy chọn) Kiểm tra mật khẩu mới không trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ.");
        }

        // 4. Băm và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}