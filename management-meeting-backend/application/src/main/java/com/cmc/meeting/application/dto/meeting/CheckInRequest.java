package com.cmc.meeting.application.dto.meeting;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotNull(message = "Room ID không được null ")
    private Long roomId;
}