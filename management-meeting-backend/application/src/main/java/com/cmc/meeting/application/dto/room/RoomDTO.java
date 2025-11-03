package com.cmc.meeting.application.dto.room;

import lombok.Data;
import java.util.List;
import com.cmc.meeting.domain.model.Role; // Bổ sung
import com.cmc.meeting.domain.model.RoomStatus;

import java.util.Set; // Bổ sung
@Data
public class RoomDTO {
    private Long id;
    private String name;
    private int capacity;
    private String location;
    private List<String> fixedDevices;
    private Set<Role> requiredRoles;
    private RoomStatus status;
}