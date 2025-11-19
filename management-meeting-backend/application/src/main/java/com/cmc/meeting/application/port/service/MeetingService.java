package com.cmc.meeting.application.port.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cmc.meeting.application.dto.response.BookedSlotDTO;
import com.cmc.meeting.application.dto.meeting.CheckInRequest;
import com.cmc.meeting.application.dto.meeting.MeetingCancelRequest;
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
    void cancelMeeting(Long meetingId, MeetingCancelRequest request, Long currentUserId);
    MeetingDTO getMeetingById(Long meetingId, Long currentUserId);
    List<MeetingDTO> getMyMeetings(Long currentUserId);
    MeetingDTO updateMeeting(Long meetingId, MeetingUpdateRequest request, Long currentUserId);
    void respondToInvitation(Long meetingId, MeetingResponseRequest request, Long currentUserId);
    String respondByLink(String token, ParticipantStatus status);
    String checkIn(CheckInRequest request, Long currentUserId);
    void cancelMeetingSeries(String seriesId, MeetingCancelRequest request, Long currentUserId);
    // BỔ SUNG: (BS-2.1)
    MeetingDTO updateMeetingSeries(String seriesId, MeetingCreationRequest request, Long currentUserId);
    // CẬP NHẬT: (US-6) Thêm Pageable
    Page<MeetingDTO> getMyMeetings(Long currentUserId, Pageable pageable);
    Page<MeetingDTO> getAllMeetings(Pageable pageable);
    List<BookedSlotDTO> getRoomSchedule(Long roomId, LocalDateTime startTime, LocalDateTime endTime);
    List<BookedSlotDTO> getDeviceSchedule(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);
    void processMeetingApproval(Long meetingId, boolean isApproved, String reason, Long currentAdminId);
}