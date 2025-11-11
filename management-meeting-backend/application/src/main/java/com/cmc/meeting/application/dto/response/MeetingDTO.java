package com.cmc.meeting.application.dto.response;

import com.cmc.meeting.application.dto.device.DeviceDTO;
import com.cmc.meeting.domain.model.BookingStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class MeetingDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;

    // Trả về thông tin chi tiết (chúng ta sẽ tạo UserDTO và RoomDTO sau)
    private RoomDTO room;
    private UserDTO creator;
    private UserDTO organizer;
    private List<MeetingParticipantDTO> participants;

    // ----- Các DTO lồng nhau (Nested DTOs) -----
    // (Để đơn giản, chúng ta định nghĩa tạm ở đây)

    @Data
    public static class RoomDTO {
        private Long id;
        private String name;
    }

    @Data
    public static class UserDTO {
        private Long id;
        private String fullName;
    }

    private Set<DeviceDTO> devices;

    private Set<String> guestEmails;
}