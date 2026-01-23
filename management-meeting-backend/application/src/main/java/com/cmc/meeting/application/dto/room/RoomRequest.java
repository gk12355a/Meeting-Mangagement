package com.cmc.meeting.application.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import com.cmc.meeting.domain.model.Role; // Bổ sung
import com.cmc.meeting.domain.model.RoomStatus;

import java.util.Set; // Bổ sung

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class RoomRequest {

    private String name;

    @Min(value = 1, message = "Sức chứa phải ít nhất là 1")
    private Integer capacity;

    private String location;
    private String buildingName;
    private Integer floor;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> fixedDevices; // (BS-14.2)
    private Set<Role> requiredRoles;

    private RoomStatus status;
    private Boolean requiresApproval;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> deleteImages; // Danh sách URL ảnh cần xóa
}