package com.cmc.meeting.domain.model;

import lombok.Data;

@Data
public class Device {
    private Long id;
    private String name;
    private String description;
    // (Chúng ta có thể thêm 'status' (Bảo trì, Sẵn có) sau)
}