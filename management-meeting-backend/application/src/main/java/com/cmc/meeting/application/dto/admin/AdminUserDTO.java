package com.cmc.meeting.application.dto.admin;

import com.cmc.meeting.domain.model.Role;
import lombok.Data;
import java.util.Set;

@Data
public class AdminUserDTO {
    private Long id;
    private String username;
    private String fullName;
    private Set<Role> roles;
    private boolean isActive;
}