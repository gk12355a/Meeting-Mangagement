package com.cmc.meeting.application.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppConfigUpdateRequest {
    // Admin chỉ được sửa 'value', không được sửa 'key'
    @NotBlank(message = "Giá trị cấu hình không được để trống")
    private String configValue;
}