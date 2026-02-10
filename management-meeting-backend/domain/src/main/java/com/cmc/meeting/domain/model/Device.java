package com.cmc.meeting.domain.model;

import lombok.Data;

@Data
public class Device {
    private Long id;
    private String name;
    private String description;
    private DeviceStatus status = DeviceStatus.AVAILABLE;
    private java.util.List<String> images;
    private Long roomId; // Link to Room
}