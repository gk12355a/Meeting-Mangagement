package com.cmc.meeting.application.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class ContactGroupRequest {
    @NotBlank(message = "Tên nhóm không được để trống")
    private String name;
    private Set<Long> memberIds; // Danh sách ID các user trong nhóm
}