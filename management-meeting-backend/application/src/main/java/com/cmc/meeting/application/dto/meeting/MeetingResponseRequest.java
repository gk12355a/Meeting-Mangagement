package com.cmc.meeting.application.dto.meeting;

import com.cmc.meeting.domain.model.ParticipantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingResponseRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private ParticipantStatus status; // Sẽ là ACCEPTED hoặc DECLINED
}