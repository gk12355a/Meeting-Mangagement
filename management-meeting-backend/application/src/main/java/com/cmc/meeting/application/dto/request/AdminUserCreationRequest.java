package com.cmc.meeting.application.dto.request;

import com.cmc.meeting.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

/**
 * DTO cho Admin tạo người dùng mới.
 * Mật khẩu sẽ được hệ thống tự động tạo và gửi qua email.
 */
@Data
@NoArgsConstructor
public class AdminUserCreationRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email (username) không được để trống")
    @Email(message = "Email không hợp lệ")
    private String username; // Đây là email

    @NotEmpty(message = "Phải chọn ít nhất một vai trò (role)")
    private Set<Role> roles;
}