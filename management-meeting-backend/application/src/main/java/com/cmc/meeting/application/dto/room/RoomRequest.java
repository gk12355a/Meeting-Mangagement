package com.cmc.meeting.application.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoomRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    private String name;

    @Min(value = 1, message = "Sức chứa phải ít nhất là 1")
    private int capacity;

    private String location;

    private List<String> fixedDevices; // (BS-14.2)
}