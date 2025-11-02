package com.cmc.meeting.application.dto.device;

import com.cmc.meeting.domain.model.DeviceStatus;

import lombok.Data;

@Data
public class DeviceDTO {
    private Long id;
    private String name;
    private String description;
    private DeviceStatus status;
}