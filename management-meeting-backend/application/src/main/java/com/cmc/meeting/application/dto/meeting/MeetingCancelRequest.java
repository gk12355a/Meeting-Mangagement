package com.cmc.meeting.application.dto.meeting;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MeetingCancelRequest {
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}