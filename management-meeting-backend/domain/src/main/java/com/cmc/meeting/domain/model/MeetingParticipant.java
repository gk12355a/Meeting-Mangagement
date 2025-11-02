package com.cmc.meeting.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor; // Bỏ AllArgsConstructor

@Data
@NoArgsConstructor
// @AllArgsConstructor // Bỏ đi
public class MeetingParticipant {
    private User user;
    private ParticipantStatus status;
    
    // BỔ SUNG: Token bí mật cho link email
    private String responseToken; 

    // CẬP NHẬT CONSTRUCTOR
    public MeetingParticipant(User user, ParticipantStatus status, String responseToken) {
        this.user = user;
        this.status = status;
        this.responseToken = responseToken;
    }
}