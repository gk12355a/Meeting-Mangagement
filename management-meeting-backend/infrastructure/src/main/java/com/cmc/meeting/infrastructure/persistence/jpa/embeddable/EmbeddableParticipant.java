package com.cmc.meeting.infrastructure.persistence.jpa.embeddable;

import com.cmc.meeting.domain.model.ParticipantStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
@Embeddable // Đánh dấu là 1 phần của Entity khác
public class EmbeddableParticipant {

    private Long userId; // Chỉ lưu ID

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(unique = true) // Đảm bảo token là duy nhất
    private String responseToken;
}