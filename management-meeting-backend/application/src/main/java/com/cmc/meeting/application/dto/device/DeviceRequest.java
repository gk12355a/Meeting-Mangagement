package com.cmc.meeting.application.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRequest {
    @NotBlank(message = "Tên thiết bị không được để trống")
    private String name;
    private String description;
}