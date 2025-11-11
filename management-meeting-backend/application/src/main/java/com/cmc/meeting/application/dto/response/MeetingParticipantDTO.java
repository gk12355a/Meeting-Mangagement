package com.cmc.meeting.application.dto.response;

import com.cmc.meeting.domain.model.ParticipantStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một người tham gia trong ngữ cảnh một cuộc họp.
 * Bao gồm cả trạng thái (status) của họ (ACCEPTED, DECLINED, PENDING).
 */
@Data
@NoArgsConstructor
public class MeetingParticipantDTO {

    private Long id; // ID của User
    private String fullName;
    private ParticipantStatus status; // <-- TRƯỜNG BỊ THIẾU
}