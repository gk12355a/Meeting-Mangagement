package com.cmc.meeting.application.dto.group;

import com.cmc.meeting.application.dto.admin.AdminUserDTO;
import lombok.Data;
import java.util.Set;

@Data
public class ContactGroupDTO {
    private Long id;
    private String name;
    // Tái sử dụng AdminUserDTO (chỉ có id, name, username, roles, active)
    private Set<AdminUserDTO> members; 
}