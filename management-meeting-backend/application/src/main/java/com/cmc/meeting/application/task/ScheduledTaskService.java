package com.cmc.meeting.application.task;

import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final MeetingRepository meetingRepository;
    
    // Định nghĩa thời gian chờ (vd: Hủy nếu quá 15 phút)
    private static final int GRACE_PERIOD_MINUTES = 15;

    public ScheduledTaskService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

   
    // Tác vụ này tự động chạy mỗi 5 phút
    // (cron = "giây phút giờ ngày tháng ngày_trong_tuần")
    // "0 */5 * * * *" = Chạy vào giây 0, mỗi 5 phút
    
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoReleaseGhostMeetings() {
        log.info("SCHEDULER: Đang chạy tác vụ tự động giải phóng phòng...");

        // 1. Tính thời gian cutoff (vd: 15 phút trước)
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(GRACE_PERIOD_MINUTES);

        // 2. Tìm các cuộc họp "ma"
        List<Meeting> ghostMeetings = meetingRepository.findUncheckedInMeetings(cutoffTime);

        if (ghostMeetings.isEmpty()) {
            log.info("SCHEDULER: Không tìm thấy cuộc họp ma nào.");
            return;
        }

        // 3. Hủy các cuộc họp đó
        log.warn("SCHEDULER: Tìm thấy {} cuộc họp ma. Đang tiến hành hủy:", ghostMeetings.size());
        
        for (Meeting meeting : ghostMeetings) {
            log.warn("-> Hủy Meeting ID: {} (Chủ đề: {}) vì không check-in.", 
                     meeting.getId(), meeting.getTitle());
            
            // Dùng logic domain (đã viết ở US-2)
            meeting.cancelMeeting(); 
            
            // Lưu lại trạng thái CANCELLED
            meetingRepository.save(meeting);
            
            // (Bonus: Bắn event MeetingCancelledBySystemEvent để gửi mail)
        }
        
        log.info("SCHEDULER: Đã hoàn thành tác vụ.");
    }
}