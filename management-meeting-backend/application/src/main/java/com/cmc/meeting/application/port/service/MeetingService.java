package com.cmc.meeting.application.port.service;

import java.util.List;

import com.cmc.meeting.application.dto.meeting.MeetingResponseRequest;
import com.cmc.meeting.application.dto.request.MeetingCreationRequest;
import com.cmc.meeting.application.dto.request.MeetingUpdateRequest;
import com.cmc.meeting.application.dto.response.MeetingDTO;
import com.cmc.meeting.domain.model.ParticipantStatus;

// Đây là hợp đồng cho "Web" layer sử dụng
public interface MeetingService {

    /**
     * Nghiệp vụ tạo lịch họp (US-1)
     */
    MeetingDTO createMeeting(MeetingCreationRequest request, Long organizerId);

    // (Chúng ta sẽ thêm các method khác như getMeetingById, cancelMeeting... sau)
    void cancelMeeting(Long meetingId, Long currentUserId);
    MeetingDTO getMeetingById(Long meetingId, Long currentUserId);
    List<MeetingDTO> getMyMeetings(Long currentUserId);
    MeetingDTO updateMeeting(Long meetingId, MeetingUpdateRequest request, Long currentUserId);
    void respondToInvitation(Long meetingId, MeetingResponseRequest request, Long currentUserId);
    String respondByLink(String token, ParticipantStatus status);
}