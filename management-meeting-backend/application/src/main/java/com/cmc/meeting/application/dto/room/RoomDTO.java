package com.cmc.meeting.application.dto.room;

import lombok.Data;
import java.util.List;

@Data
public class RoomDTO {
    private Long id;
    private String name;
    private int capacity;
    private String location;
    private List<String> fixedDevices;
}