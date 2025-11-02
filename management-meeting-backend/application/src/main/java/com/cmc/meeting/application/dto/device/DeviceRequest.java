package com.cmc.meeting.application.dto.device;

import com.cmc.meeting.domain.model.DeviceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRequest {
    @NotBlank(message = "Tên thiết bị không được để trống")
    private String name;
    private String description;
    @NotNull(message = "Trạng thái không được để trống")
    private DeviceStatus status;
}