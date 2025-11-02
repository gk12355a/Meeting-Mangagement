package com.cmc.meeting.application.dto.admin;

import com.cmc.meeting.domain.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class AdminUserUpdateRequest {
    @NotNull
    private Set<Role> roles; // Admin có thể đổi quyền (vd: USER -> ADMIN)

    @NotNull
    private Boolean isActive; // Admin có thể vô hiệu hóa
}