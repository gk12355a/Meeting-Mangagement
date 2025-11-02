package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.report.RoomUsageReportDTO;
import com.cmc.meeting.application.port.service.ReportService;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration; // Bổ sung
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // Toàn bộ service này là ReadOnly
public class ReportServiceImpl implements ReportService {

    private final MeetingRepository meetingRepository;

    public ReportServiceImpl(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Override
    public List<RoomUsageReportDTO> getRoomUsageReport(LocalDate fromDate, LocalDate toDate) {

        // 1. Chuyển đổi sang LocalDateTime (từ 00:00 ngày bắt đầu đến 23:59 ngày kết thúc)
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        // 2. Lấy dữ liệu thô từ CSDL
        List<Meeting> confirmedMeetings = meetingRepository.findConfirmedMeetingsInDateRange(from, to);

        // 3. Xử lý (Tổng hợp) bằng Java Stream
        // Nhóm tất cả cuộc họp theo Phòng
        Map<Room, List<Meeting>> meetingsByRoom = confirmedMeetings.stream()
                .collect(Collectors.groupingBy(Meeting::getRoom));

        // 4. Tính toán và tạo DTO
        return meetingsByRoom.entrySet().stream()
                .map(entry -> {
                    Room room = entry.getKey();
                    List<Meeting> meetingsInRoom = entry.getValue();

                    // Tính tổng số giờ
                    double totalHours = meetingsInRoom.stream()
                            .mapToDouble(meeting -> {
                                // Tính số phút và chia cho 60.0
                                Duration duration = Duration.between(
                                        meeting.getStartTime(), 
                                        meeting.getEndTime());
                                return duration.toMinutes() / 60.0;
                            })
                            .sum();

                    // Tạo DTO báo cáo
                    RoomUsageReportDTO dto = new RoomUsageReportDTO();
                    dto.setRoomId(room.getId());
                    dto.setRoomName(room.getName());
                    dto.setBookingCount(meetingsInRoom.size());
                    dto.setTotalHoursBooked(totalHours);

                    return dto;
                })
                .collect(Collectors.toList());
    }
}